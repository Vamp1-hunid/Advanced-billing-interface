package lodgebilling.service;

import lodgebilling.data.AuditLog;
import lodgebilling.data.AuditLog.Action;
import lodgebilling.data.Database;
import lodgebilling.data.RoomRepository;
import lodgebilling.data.SettingsRepository;
import lodgebilling.model.Money;
import lodgebilling.model.RateCard;
import lodgebilling.model.Room;
import lodgebilling.model.RoomStatus;
import lodgebilling.model.RoomType;
import lodgebilling.security.Permission;
import lodgebilling.security.Session;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * The front-desk rules. For now: the rooms, their housekeeping status and the prices.
 * Every method that changes something checks the user's permission first and
 * writes an audit entry, so the screens cannot skip either.
 */
public class LodgeService {

    /** Zambia's standard VAT rate, used until an admin sets another. */
    public static final BigDecimal DEFAULT_VAT = new BigDecimal("16");
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000");

    private final Database database;
    private final RoomRepository rooms;
    private final SettingsRepository settings;
    private final AuditLog audit;
    private final Clock clock;
    private final RateCard rateCard;

    public LodgeService(Database database, AuditLog audit, Clock clock) {
        this.database = database;
        this.rooms = new RoomRepository(database);
        this.settings = new SettingsRepository(database);
        this.audit = audit;
        this.clock = clock;
        seedIfEmpty();
        this.rateCard = settings.loadRateCard(DEFAULT_VAT);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
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
}
