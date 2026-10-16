package lodgebilling.model;

/** One physical room, e.g. "201", a Deluxe room that is currently Dirty. */
public record Room(long id, String number, RoomType type, RoomStatus status) {
    public String getNumber()     { return number; }
    public RoomType getType()     { return type; }
    public RoomStatus getStatus() { return status; }

    @Override
    public String toString() {
        return number + " (" + type + ")";
    }
}
