package lodgebilling.model;

/** Housekeeping state of a room. OCCUPIED is set by check-in, DIRTY by check-out. */
public enum RoomStatus {
    AVAILABLE("Available"),
    OCCUPIED("Occupied"),
    DIRTY("Dirty"),
    MAINTENANCE("Maintenance");

    private final String label;

    RoomStatus(String label) {
        this.label = label;
    }

    /** CSS class used for the coloured status chip (green / red / yellow / grey). */
    public String styleClass() {
        return "status-" + name().toLowerCase();
    }

    @Override
    public String toString() {
        return label;
    }
}
