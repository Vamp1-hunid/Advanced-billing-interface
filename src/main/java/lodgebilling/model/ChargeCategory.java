package lodgebilling.model;

/** Extras that can be posted to a guest's folio. Room nights are worked out, not posted. */
public enum ChargeCategory {
    BAR("Bar"),
    RESTAURANT("Restaurant"),
    LAUNDRY("Laundry"),
    TRANSPORT("Transport"),
    OTHER("Other");

    private final String label;

    ChargeCategory(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
