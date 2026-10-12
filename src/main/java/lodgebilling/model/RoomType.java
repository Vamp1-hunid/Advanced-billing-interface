package lodgebilling.model;

import java.math.BigDecimal;

/**
 * The room types the lodge offers, each with its price per night.
 * Keeping prices here (not scattered in the UI) means one place to change them.
 */
public enum RoomType {
    STANDARD("Standard", "350.00"),
    DELUXE("Deluxe", "550.00"),
    FAMILY("Family", "750.00");

    private final String label;
    private final BigDecimal nightlyRate;

    RoomType(String label, String nightlyRate) {
        this.label = label;
        this.nightlyRate = new BigDecimal(nightlyRate);
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getNightlyRate() {
        return nightlyRate;
    }

    /** The ComboBox and the table show this text. */
    @Override
    public String toString() {
        return label;
    }
}
