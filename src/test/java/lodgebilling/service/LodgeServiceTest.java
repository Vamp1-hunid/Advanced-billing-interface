package lodgebilling.service;

import lodgebilling.data.AuditLog;
import lodgebilling.data.Database;
import lodgebilling.model.ChargeCategory;
import lodgebilling.model.FolioEntry;
import lodgebilling.model.FolioSummary;
import lodgebilling.model.Payer;
import lodgebilling.model.PaymentMethod;
import lodgebilling.model.Reservation;
import lodgebilling.model.ReservationForm;
import lodgebilling.model.ReservationStatus;
import lodgebilling.model.Room;
import lodgebilling.model.RoomStatus;
import lodgebilling.model.RoomType;
import lodgebilling.model.User;
import lodgebilling.security.AccessDeniedException;
import lodgebilling.security.Role;
import lodgebilling.security.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LodgeServiceTest {

    private static final Session ADMIN = new Session(new User(1, "boss", "Boss", Role.ADMIN, true, false));
    private static final Session DESK = new Session(new User(2, "mary", "Mary", Role.STAFF, true, false));
    private static final Session CLEANER = new Session(new User(3, "joe", "Joe", Role.HOUSEKEEPING, true, false));

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    private Database db;
    private LodgeService lodge;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        Clock clock = Clock.fixed(Instant.parse("2026-10-12T09:00:00Z"), ZoneOffset.UTC);
        lodge = new LodgeService(db, new AuditLog(db, clock), clock);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private Room room(String number) {
        return lodge.rooms().stream().filter(r -> r.number().equals(number)).findFirst().orElseThrow();
    }

    private ReservationForm form(String guest, String roomNo, LocalDate in, LocalDate out) {
        return new ReservationForm(guest, "+260 97 123 4567", "", false, room(roomNo), in, out, "");
    }

    private static BigDecimal k(String amount) {
        return new BigDecimal(amount);
    }

    @Test
    void firstRunSeedsRoomsRatesAndVat() {
        assertEquals(9, lodge.rooms().size());
        assertEquals(k("550.00"), lodge.rateCard().rateFor(RoomType.DELUXE));
        assertEquals(0, k("16").compareTo(lodge.rateCard().vatPercent()));
    }

    @Test
    void overlappingBookingForSameRoomIsRefusedButBackToBackIsFine() throws LodgeException {
        lodge.book(DESK, form("John Phiri", "201", TODAY, TODAY.plusDays(3)));
        // Overlaps night 2
        assertThrows(LodgeException.class,
                () -> lodge.book(DESK, form("Mary Banda", "201", TODAY.plusDays(2), TODAY.plusDays(4))));
        // Arrives the morning John leaves: allowed
        lodge.book(DESK, form("Mary Banda", "201", TODAY.plusDays(3), TODAY.plusDays(5)));
        assertFalse(lodge.freeRooms(TODAY, TODAY.plusDays(1), 0).contains(room("201")));
    }

    @Test
    void badBookingsAreRefused() {
        assertThrows(LodgeException.class, () -> lodge.book(DESK, form("John", "201", TODAY.minusDays(1), TODAY)));
        assertThrows(LodgeException.class, () -> lodge.book(DESK, form("John", "201", TODAY, TODAY)));
        assertThrows(LodgeException.class, () -> lodge.book(DESK, form("J0hn", "201", TODAY, TODAY.plusDays(1))));
        assertThrows(LodgeException.class, () -> lodge.book(DESK,
                form("John", "201", TODAY, TODAY.plusDays(LodgeService.MAX_NIGHTS + 1))));
    }

    @Test
    void housekeepingCannotBookOrSeeMoney() {
        assertThrows(AccessDeniedException.class,
                () -> lodge.book(CLEANER, form("John Phiri", "201", TODAY, TODAY.plusDays(1))));
        assertThrows(AccessDeniedException.class,
                () -> lodge.recordPayment(CLEANER, 1, PaymentMethod.CASH, k("10"), Payer.GUEST, ""));
    }

    @Test
    void fullStayWithVatChargesPaymentsAndCheckout() throws LodgeException {
        Reservation r = lodge.walkIn(DESK, form("John Phiri", "201", TODAY, TODAY.plusDays(1)));
        assertEquals(ReservationStatus.CHECKED_IN, r.status());
        assertEquals(RoomStatus.OCCUPIED, room("201").status());

        lodge.postCharge(DESK, r.id(), ChargeCategory.BAR, "2 x Mosi", k("50"), Payer.GUEST);
        FolioSummary s = lodge.folio(r.id()).summary();
        // Deluxe 1 x 550, + 50 bar = 600, VAT 16% = 96, total 696
        assertEquals(k("600.00"), s.subtotal());
        assertEquals(k("96.00"), s.vat());
        assertEquals(k("696.00"), s.total());

        // Cannot leave while owing money
        BalanceDueException due = assertThrows(BalanceDueException.class, () -> lodge.checkOut(DESK, r.id(), false));
        assertEquals(k("696.00"), due.balance());
        // Overpaying (typo) is refused
        assertThrows(LodgeException.class,
                () -> lodge.recordPayment(DESK, r.id(), PaymentMethod.CASH, k("6960"), Payer.GUEST, ""));

        lodge.recordPayment(DESK, r.id(), PaymentMethod.MOBILE_MONEY, k("696.00"), Payer.GUEST, "MTN ref 77");
        LodgeService.Folio done = lodge.checkOut(DESK, r.id(), false);
        assertEquals(ReservationStatus.CHECKED_OUT, done.stay().status());
        assertTrue(done.summary().settled());
        assertEquals(RoomStatus.DIRTY, room("201").status());   // housekeeping's turn
    }

    @Test
    void leavingEarlyBillsOnlyNightsUsedAndRefundsTheChange() throws LodgeException {
        // Booked 3 nights, paid in full up front, leaves on the first day
        Reservation r = lodge.walkIn(DESK, form("John Phiri", "101", TODAY, TODAY.plusDays(3)));
        BigDecimal planned = lodge.folio(r.id()).summary().total();        // 3 x 350 + 16% = 1218
        assertEquals(k("1218.00"), planned);
        lodge.recordPayment(DESK, r.id(), PaymentMethod.CASH, planned, Payer.GUEST, "");

        LodgeService.Folio done = lodge.checkOut(DESK, r.id(), false);
        assertEquals(1, done.stay().nights());
        assertEquals(k("406.00"), done.summary().total());                 // 1 x 350 + 16%
        assertTrue(done.entries().stream().anyMatch(e -> e.isRefund() && e.amount().equals(k("812.00"))));
        assertEquals(0, done.summary().balance().signum());
    }

    @Test
    void dirtyRoomCannotBeCheckedIntoUntilHousekeepingCleansIt() throws LodgeException {
        lodge.setRoomStatus(CLEANER, room("101"), RoomStatus.DIRTY);
        Reservation r = lodge.book(DESK, form("John Phiri", "101", TODAY, TODAY.plusDays(1)));
        assertThrows(LodgeException.class, () -> lodge.checkIn(DESK, r.id()));
        lodge.setRoomStatus(CLEANER, room("101"), RoomStatus.AVAILABLE);
        assertEquals(ReservationStatus.CHECKED_IN, lodge.checkIn(DESK, r.id()).status());
        // An occupied room's status cannot be changed by hand
        assertThrows(LodgeException.class, () -> lodge.setRoomStatus(CLEANER, room("101"), RoomStatus.AVAILABLE));
    }

    @Test
    void splitBillCompanyPaysRoomGuestPaysBar() throws LodgeException {
        ReservationForm f = new ReservationForm("John Phiri", "", "Zambeef Ltd", true, room("101"),
                TODAY, TODAY.plusDays(1), "");
        Reservation r = lodge.walkIn(DESK, f);
        lodge.postCharge(DESK, r.id(), ChargeCategory.BAR, "", k("100"), Payer.GUEST);

        FolioSummary s = lodge.folio(r.id()).summary();
        FolioSummary.PayerLine guest = s.payers().stream().filter(p -> p.payer() == Payer.GUEST).findFirst().orElseThrow();
        FolioSummary.PayerLine company = s.payers().stream().filter(p -> p.payer() == Payer.COMPANY).findFirst().orElseThrow();
        assertEquals(k("116.00"), guest.total());      // 100 + 16% VAT
        assertEquals(k("406.00"), company.total());    // 350 + 16% VAT
        assertEquals(s.total(), guest.total().add(company.total()));

        // Company cannot be made to pay more than its share
        assertThrows(LodgeException.class, () -> lodge.recordSplitPayment(DESK, r.id(),
                PaymentMethod.CASH, k("116"), PaymentMethod.BANK_TRANSFER, k("500"), ""));
        assertEquals(0, lodge.folio(r.id()).summary().paid().signum(), "refused split must save nothing");

        lodge.recordSplitPayment(DESK, r.id(), PaymentMethod.CASH, k("116"), PaymentMethod.BANK_TRANSFER, k("406"), "");
        assertTrue(lodge.folio(r.id()).summary().settled());
    }

    @Test
    void onlyAdminCanForceCheckoutOrVoid() throws LodgeException {
        Reservation r = lodge.walkIn(DESK, form("John Phiri", "102", TODAY, TODAY.plusDays(1)));
        FolioEntry bar = lodge.postCharge(DESK, r.id(), ChargeCategory.BAR, "", k("40"), Payer.GUEST);

        assertThrows(AccessDeniedException.class, () -> lodge.voidEntry(DESK, bar, "wrong room"));
        lodge.voidEntry(ADMIN, bar, "posted to wrong room");
        assertEquals(k("0.00"), lodge.folio(r.id()).summary().extras());

        assertThrows(AccessDeniedException.class, () -> lodge.checkOut(DESK, r.id(), true));
        LodgeService.Folio gone = lodge.checkOut(ADMIN, r.id(), true);
        assertEquals(ReservationStatus.CHECKED_OUT, gone.stay().status());
        assertTrue(lodge.folio(r.id()).summary().balance().signum() > 0);   // the debt is still on record
    }

    @Test
    void rateChangesDoNotRewriteExistingBookings() throws LodgeException {
        Reservation before = lodge.book(DESK, form("John Phiri", "301", TODAY, TODAY.plusDays(1)));
        Map<RoomType, BigDecimal> rates = new EnumMap<>(RoomType.class);
        rates.put(RoomType.STANDARD, k("400"));
        rates.put(RoomType.DELUXE, k("600"));
        rates.put(RoomType.FAMILY, k("900"));
        assertThrows(AccessDeniedException.class, () -> lodge.saveRates(DESK, rates, k("16")));
        lodge.saveRates(ADMIN, rates, k("0"));

        assertEquals(k("750.00"), lodge.reservation(before.id()).nightlyRate());   // old price kept
        Reservation after = lodge.book(DESK, form("Mary Banda", "302", TODAY, TODAY.plusDays(1)));
        assertEquals(k("900.00"), after.nightlyRate());
        assertEquals(0, after.vatPercent().signum());
    }

    @Test
    void cancellingWithDepositNeedsVoidFirst() throws LodgeException {
        Reservation r = lodge.book(DESK, form("John Phiri", "103", TODAY.plusDays(5), TODAY.plusDays(6)));
        FolioEntry deposit = lodge.recordPayment(DESK, r.id(), PaymentMethod.CASH, k("100"), Payer.GUEST, "deposit");
        assertThrows(LodgeException.class, () -> lodge.cancel(DESK, r.id()));
        lodge.voidEntry(ADMIN, deposit, "refunded in cash");
        lodge.cancel(DESK, r.id());
        assertEquals(ReservationStatus.CANCELLED, lodge.reservation(r.id()).status());
        assertTrue(lodge.freeRooms(TODAY.plusDays(5), TODAY.plusDays(6), 0).contains(room("103")));
    }

    @Test
    void checkOutFreesTheRoomAsDirtyAndLeavingEarlyShortensTheStay() throws LodgeException {
        Reservation r = lodge.walkIn(DESK, form("John Phiri", "102", TODAY, TODAY.plusDays(3)));
        assertEquals(RoomStatus.OCCUPIED, room("102").status());
        lodge.recordPayment(DESK, r.id(), PaymentMethod.CASH, lodge.folio(r.id()).summary().balance(), Payer.GUEST, "");
        Reservation gone = lodge.checkOut(DESK, r.id(), false).stay();
        assertEquals(ReservationStatus.CHECKED_OUT, gone.status());
        assertEquals(TODAY.plusDays(1), gone.checkOut());   // one night used, not three
        assertEquals(RoomStatus.DIRTY, room("102").status());
    }

    @Test
    void aCancelledBookingFreesTheRoom() throws LodgeException {
        Reservation r = lodge.book(DESK, form("John Phiri", "103", TODAY.plusDays(5), TODAY.plusDays(6)));
        lodge.cancel(DESK, r.id());
        assertEquals(ReservationStatus.CANCELLED, lodge.reservation(r.id()).status());
        assertTrue(lodge.freeRooms(TODAY.plusDays(5), TODAY.plusDays(6), 0).contains(room("103")));
    }
}
