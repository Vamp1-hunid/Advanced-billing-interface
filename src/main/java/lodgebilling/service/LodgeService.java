package lodgebilling.service;

import lodgebilling.data.AuditLog;
import lodgebilling.data.AuditLog.Action;
import lodgebilling.data.Database;
import lodgebilling.data.RoomRepository;
import lodgebilling.model.Room;
import lodgebilling.model.RoomStatus;
import lodgebilling.model.RoomType;
import lodgebilling.security.Permission;
import lodgebilling.security.Session;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * The front-desk rules. For now: the rooms and their housekeeping status.
 * Every method that changes something checks the user's permission first and
 * writes an audit entry, so the screens cannot skip either.
 */
public class LodgeService {

    private final Database database;
    private final RoomRepository rooms;
    private final AuditLog audit;
    private final Clock clock;

    public LodgeService(Database database, AuditLog audit, Clock clock) {
        this.database = database;
        this.rooms = new RoomRepository(database);
        this.audit = audit;
        this.clock = clock;
        seedIfEmpty();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** First run: a small lodge with nine rooms. */
    private void seedIfEmpty() {
        if (rooms.count() == 0) {
            database.inTransaction(() -> {
                for (String n : List.of("101", "102", "103", "104")) rooms.insert(n, RoomType.STANDARD);
                for (String n : List.of("201", "202", "203")) rooms.insert(n, RoomType.DELUXE);
                for (String n : List.of("301", "302")) rooms.insert(n, RoomType.FAMILY);
                return null;
            });
        }
    }

    // =====================================================================
    // Rooms
    // =====================================================================

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
