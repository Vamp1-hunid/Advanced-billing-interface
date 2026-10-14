package lodgebilling.service;

import lodgebilling.data.AuditLog;
import lodgebilling.data.Database;
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
    void housekeepingCannotBook() {
        assertThrows(AccessDeniedException.class,
                () -> lodge.book(CLEANER, form("John Phiri", "201", TODAY, TODAY.plusDays(1))));
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
    void checkOutFreesTheRoomAsDirtyAndLeavingEarlyShortensTheStay() throws LodgeException {
        Reservation r = lodge.walkIn(DESK, form("John Phiri", "102", TODAY, TODAY.plusDays(3)));
        assertEquals(RoomStatus.OCCUPIED, room("102").status());
        Reservation gone = lodge.checkOut(DESK, r.id());
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
