package lodgebilling.service;

import lodgebilling.data.AuditLog;
import lodgebilling.data.AuditLog.Action;
import lodgebilling.data.Database;
import lodgebilling.data.ReservationRepository;
import lodgebilling.data.RoomRepository;
import lodgebilling.data.SettingsRepository;
import lodgebilling.model.Money;
import lodgebilling.model.RateCard;
import lodgebilling.model.Reservation;
import lodgebilling.model.ReservationForm;
import lodgebilling.model.ReservationStatus;
import lodgebilling.model.Room;
import lodgebilling.model.RoomStatus;
import lodgebilling.model.RoomType;
import lodgebilling.security.Permission;
import lodgebilling.security.Session;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The front-desk rules: rooms, prices, bookings and check-in/out.
 * Every method that changes something checks the user's permission first and
 * writes an audit entry, so the screens cannot skip either.
 */
public class LodgeService {

    /** Zambia's standard VAT rate, used until an admin sets another. */
    public static final BigDecimal DEFAULT_VAT = new BigDecimal("16");
    public static final int MAX_NIGHTS = 60;
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private static final String NAME_PATTERN = "\\p{L}+([ '’\\-.]\\p{L}+)*";
    private static final String PHONE_PATTERN = "[+0-9 ()\\-]{0,20}";

    private final Database database;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final SettingsRepository settings;
    private final AuditLog audit;
    private final Clock clock;
    private final RateCard rateCard;

    public LodgeService(Database database, AuditLog audit, Clock clock) {
        this.database = database;
        this.rooms = new RoomRepository(database);
        this.reservations = new ReservationRepository(database);
        this.settings = new SettingsRepository(database);
        this.audit = audit;
        this.clock = clock;
        seedIfEmpty();
        this.rateCard = settings.loadRateCard(DEFAULT_VAT);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    /** First run: a small lodge with nine rooms and the original prices. */
    private void seedIfEmpty() {
        if (rooms.count() == 0) {
            database.inTransaction(() -> {
                for (String n : List.of("101", "102", "103", "104")) rooms.insert(n, RoomType.STANDARD);
                for (String n : List.of("201", "202", "203")) rooms.insert(n, RoomType.DELUXE);
                for (String n : List.of("301", "302")) rooms.insert(n, RoomType.FAMILY);
                for (RoomType t : RoomType.values()) settings.saveRate(t, t.getNightlyRate());
                settings.put(SettingsRepository.VAT_PERCENT, DEFAULT_VAT.toPlainString());
                return null;
            });
        }
    }

    // =====================================================================
    // Rates and rooms
    // =====================================================================

    /** The live rate card; the same object is updated in place when an admin changes prices. */
    public RateCard rateCard() {
        return rateCard;
    }

    public void saveRates(Session session, Map<RoomType, BigDecimal> newRates, BigDecimal vatPercent)
            throws LodgeException {
        session.require(Permission.MANAGE_ROOMS);
        for (RoomType t : RoomType.values()) {
            BigDecimal r = newRates.get(t);
            if (r == null || r.signum() <= 0 || r.compareTo(MAX_AMOUNT) > 0) {
                throw new LodgeException("Enter a price above zero for " + t + " rooms.");
            }
        }
        if (vatPercent == null || vatPercent.signum() < 0 || vatPercent.compareTo(new BigDecimal("50")) > 0) {
            throw new LodgeException("VAT must be between 0 and 50 %.");
        }
        database.inTransaction(() -> {
            newRates.forEach(settings::saveRate);
            settings.put(SettingsRepository.VAT_PERCENT, vatPercent.stripTrailingZeros().toPlainString());
            return null;
        });
        rateCard.replaceWith(new RateCard(newRates, vatPercent));
        StringBuilder detail = new StringBuilder();
        newRates.forEach((t, r) -> detail.append(t).append(' ').append(Money.format(r)).append(", "));
        audit.record(session.username(), Action.RATES_CHANGED, detail + "VAT " + vatPercent + "%");
    }

    public List<Room> rooms() {
        return rooms.findAll();
    }

    public Room addRoom(Session session, String number, RoomType type) throws LodgeException {
        session.require(Permission.MANAGE_ROOMS);
        String n = number == null ? "" : number.trim();
        if (!n.matches("[A-Za-z0-9\\-]{1,10}")) {
            throw new LodgeException("Room numbers are 1 to 10 letters, digits or hyphens (e.g. 104 or A-2).");
        }
        if (type == null) {
            throw new LodgeException("Choose a room type.");
        }
        if (rooms.numberExists(n)) {
            throw new LodgeException("Room " + n + " already exists.");
        }
        Room room = rooms.insert(n, type);
        audit.record(session.username(), Action.ROOM_ADDED, n + " (" + type + ")");
        return room;
    }

    /** Housekeeping: Available / Dirty / Maintenance. Occupied is set only by check-in. */
    public void setRoomStatus(Session session, Room room, RoomStatus status) throws LodgeException {
        session.require(Permission.UPDATE_ROOM_STATUS);
        Room current = rooms.findById(room.id()).orElseThrow(() -> new LodgeException("That room no longer exists."));
        if (status == RoomStatus.OCCUPIED) {
            throw new LodgeException("A room becomes Occupied when a guest checks in.");
        }
        if (current.status() == RoomStatus.OCCUPIED) {
            throw new LodgeException("Room " + current.number() + " has a guest in it. Check the guest out first.");
        }
        rooms.setStatus(current.id(), status);
        audit.record(session.username(), Action.ROOM_STATUS, current.number() + ": " + current.status() + " -> " + status);
    }

    // =====================================================================
    // Bookings
    // =====================================================================

    public List<Reservation> reservations() {
        return reservations.findAll();
    }

    public List<Reservation> reservationsBetween(LocalDate from, LocalDate to) {
        return reservations.findOverlapping(from, to);
    }

    public Reservation reservation(long id) throws LodgeException {
        return reservations.findById(id).orElseThrow(() -> new LodgeException("That booking no longer exists."));
    }

    /** Rooms free for every night of the stay (ignoring the booking being edited, if any). */
    public List<Room> freeRooms(LocalDate checkIn, LocalDate checkOut, long ignoreReservationId) {
        List<Room> free = new ArrayList<>();
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            return free;
        }
        for (Room r : rooms.findAll()) {
            if (!reservations.roomTaken(r.id(), checkIn, checkOut, ignoreReservationId)) {
                free.add(r);
            }
        }
        return free;
    }

    public Reservation book(Session session, ReservationForm form) throws LodgeException {
        session.require(Permission.MANAGE_RESERVATIONS);
        ReservationForm f = clean(form);
        checkForm(f, 0);
        if (f.checkIn().isBefore(today())) {
            throw new LodgeException("The check-in date is in the past.");
        }
        long id = reservations.insert(f.guestName(), f.phone(), f.company(), f.companyPaysRoom(), f.room().id(),
                f.checkIn(), f.checkOut(), rateCard.rateFor(f.room().type()), rateCard.vatPercent(), f.notes(),
                session.username(), now());
        Reservation r = reservation(id);
        audit.record(session.username(), Action.BOOKING_CREATED, describe(r));
        return r;
    }

    /** A guest arriving without a booking: book from today and check in straight away. */
    public Reservation walkIn(Session session, ReservationForm form) throws LodgeException {
        session.require(Permission.MANAGE_RESERVATIONS);
        ReservationForm f = clean(form);
        if (!today().equals(f.checkIn())) {
            throw new LodgeException("A walk-in starts today.");
        }
        checkRoomReady(f.room());
        Reservation booked = book(session, f);
        return checkIn(session, booked.id());
    }

    /**
     * Before arrival everything can change. Once the guest is in, only their
     * details and the departure date can (the room and arrival are history).
     */
    public Reservation update(Session session, long id, ReservationForm form) throws LodgeException {
        session.require(Permission.MANAGE_RESERVATIONS);
        Reservation old = reservation(id);
        ReservationForm f = clean(form);
        if (old.status() == ReservationStatus.CHECKED_IN) {
            if (f.room().id() != old.room().id() || !f.checkIn().equals(old.checkIn())) {
                throw new LodgeException("The guest is already in. Only their details and departure date can change.");
            }
            if (f.checkOut().isBefore(today())) {
                throw new LodgeException("The departure date cannot be in the past.");
            }
        } else if (old.status() != ReservationStatus.BOOKED) {
            throw new LodgeException("A " + old.status().toString().toLowerCase() + " stay cannot be changed.");
        }
        checkForm(f, id);
        reservations.updateDetails(id, f.guestName(), f.phone(), f.company(), f.companyPaysRoom(), f.room().id(),
                f.checkIn(), f.checkOut(), f.notes());
        Reservation r = reservation(id);
        audit.record(session.username(), Action.BOOKING_UPDATED, describe(r));
        return r;
    }

    public void cancel(Session session, long id) throws LodgeException {
        session.require(Permission.MANAGE_RESERVATIONS);
        Reservation r = reservation(id);
        if (r.status() != ReservationStatus.BOOKED) {
            throw new LodgeException("Only a booking that has not arrived yet can be cancelled.");
        }
        reservations.setStatus(id, ReservationStatus.CANCELLED);
        audit.record(session.username(), Action.BOOKING_CANCELLED, describe(r));
    }

    public Reservation checkIn(Session session, long id) throws LodgeException {
        session.require(Permission.MANAGE_RESERVATIONS);
        Reservation r = reservation(id);
        if (r.status() != ReservationStatus.BOOKED) {
            throw new LodgeException("Only a booked stay can be checked in (this one is " + r.status() + ").");
        }
        if (r.checkIn().isAfter(today())) {
            throw new LodgeException("This booking starts on " + r.checkIn() + ". Change the dates to check in early.");
        }
        if (!r.checkOut().isAfter(today())) {
            throw new LodgeException("This booking ended on " + r.checkOut() + ". Change the dates first.");
        }
        Room room = rooms.findById(r.room().id()).orElseThrow();
        checkRoomReady(room);
        database.inTransaction(() -> {
            reservations.setStatus(id, ReservationStatus.CHECKED_IN);
            rooms.setStatus(room.id(), RoomStatus.OCCUPIED);
            return null;
        });
        audit.record(session.username(), Action.CHECK_IN, describe(r));
        return reservation(id);
    }

    /**
     * Checks the guest out. Leaving early shortens the stay to the nights used
     * (at least one). The room becomes Dirty until housekeeping cleans it.
     */
    public Reservation checkOut(Session session, long id) throws LodgeException {
        session.require(Permission.MANAGE_RESERVATIONS);
        Reservation r = reservation(id);
        if (r.status() != ReservationStatus.CHECKED_IN) {
            throw new LodgeException("Only a guest who is in house can be checked out.");
        }
        LocalDate leaving = today().isAfter(r.checkIn()) ? today() : r.checkIn().plusDays(1);
        LocalDate newCheckOut = leaving.isBefore(r.checkOut()) ? leaving : r.checkOut();
        database.inTransaction(() -> {
            if (!newCheckOut.equals(r.checkOut())) {
                reservations.updateDetails(id, r.guestName(), r.guestPhone(), r.company(), r.companyPaysRoom(),
                        r.room().id(), r.checkIn(), newCheckOut, r.notes());
            }
            reservations.setStatus(id, ReservationStatus.CHECKED_OUT);
            rooms.setStatus(r.room().id(), RoomStatus.DIRTY);
            return null;
        });
        audit.record(session.username(), Action.CHECK_OUT, describe(r));
        return reservation(id);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private void checkForm(ReservationForm f, long ignoreId) throws LodgeException {
        if (f.guestName().length() < 2 || f.guestName().length() > 60 || !f.guestName().matches(NAME_PATTERN)) {
            throw new LodgeException("Enter the guest's name (letters, spaces, hyphens, apostrophes).");
        }
        if (!f.phone().matches(PHONE_PATTERN)) {
            throw new LodgeException("The phone number may contain digits, spaces, +, - and brackets only.");
        }
        if (f.company().length() > 60) {
            throw new LodgeException("The company name is too long (60 characters at most).");
        }
        if (f.companyPaysRoom() && f.company().isBlank()) {
            throw new LodgeException("Enter the company name, or untick \"Company pays the room\".");
        }
        if (f.room() == null) {
            throw new LodgeException("Choose a room.");
        }
        if (f.checkIn() == null || f.checkOut() == null) {
            throw new LodgeException("Choose the arrival and departure dates.");
        }
        if (!f.checkOut().isAfter(f.checkIn())) {
            throw new LodgeException("The departure date must be after the arrival date.");
        }
        long nights = java.time.temporal.ChronoUnit.DAYS.between(f.checkIn(), f.checkOut());
        if (nights > MAX_NIGHTS) {
            throw new LodgeException("A stay can be at most " + MAX_NIGHTS + " nights.");
        }
        if (reservations.roomTaken(f.room().id(), f.checkIn(), f.checkOut(), ignoreId)) {
            throw new LodgeException("Room " + f.room().number() + " is already booked for some of those nights.");
        }
    }

    private static void checkRoomReady(Room room) throws LodgeException {
        switch (room.status()) {
            case AVAILABLE -> { }
            case DIRTY -> throw new LodgeException("Room " + room.number()
                    + " has not been cleaned yet. Housekeeping must mark it Available first.");
            case MAINTENANCE -> throw new LodgeException("Room " + room.number() + " is under maintenance.");
            case OCCUPIED -> throw new LodgeException("Room " + room.number() + " still has a guest in it.");
        }
    }

    private static String describe(Reservation r) {
        return r.reference() + " " + r.guestName() + ", room " + r.room().number() + ", "
                + r.checkIn() + " to " + r.checkOut();
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    private static ReservationForm clean(ReservationForm f) {
        return new ReservationForm(clean(f.guestName()), clean(f.phone()), clean(f.company()),
                f.companyPaysRoom(), f.room(), f.checkIn(), f.checkOut(), clean(f.notes()));
    }
}
