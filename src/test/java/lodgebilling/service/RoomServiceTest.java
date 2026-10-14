package lodgebilling.service;

import lodgebilling.data.AuditLog;
import lodgebilling.data.Database;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Rooms and housekeeping: who may change what, and the rules on room status. */
class RoomServiceTest {

    private static final Session ADMIN = new Session(new User(1, "boss", "Boss", Role.ADMIN, true, false));
    private static final Session DESK = new Session(new User(2, "mary", "Mary", Role.STAFF, true, false));
    private static final Session CLEANER = new Session(new User(3, "joe", "Joe", Role.HOUSEKEEPING, true, false));

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

    @Test
    void firstRunSeedsNineAvailableRooms() {
        assertEquals(9, lodge.rooms().size());
        assertEquals(RoomType.FAMILY, room("302").type());
        assertEquals(9, lodge.rooms().stream().filter(r -> r.status() == RoomStatus.AVAILABLE).count());
    }

    @Test
    void housekeepingCanMarkRoomsDirtyAndClean() throws LodgeException {
        lodge.setRoomStatus(CLEANER, room("101"), RoomStatus.DIRTY);
        assertEquals(RoomStatus.DIRTY, room("101").status());
        lodge.setRoomStatus(CLEANER, room("101"), RoomStatus.AVAILABLE);
        assertEquals(RoomStatus.AVAILABLE, room("101").status());
    }

    @Test
    void occupiedIsNeverSetByHand() {
        assertThrows(LodgeException.class, () -> lodge.setRoomStatus(DESK, room("101"), RoomStatus.OCCUPIED));
    }

    @Test
    void onlyAnAdminAddsRoomsAndNumbersMustBeNew() throws LodgeException {
        assertThrows(AccessDeniedException.class, () -> lodge.addRoom(DESK, "105", RoomType.STANDARD));
        assertThrows(AccessDeniedException.class, () -> lodge.addRoom(CLEANER, "105", RoomType.STANDARD));
        lodge.addRoom(ADMIN, "105", RoomType.STANDARD);
        assertEquals(10, lodge.rooms().size());
        assertThrows(LodgeException.class, () -> lodge.addRoom(ADMIN, "105", RoomType.DELUXE));
        assertThrows(LodgeException.class, () -> lodge.addRoom(ADMIN, "1 0 5", RoomType.DELUXE));
    }
}
