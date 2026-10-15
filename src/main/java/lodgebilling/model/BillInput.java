package lodgebilling.model;

import java.math.BigDecimal;

/**
 * The values from the form once they have passed validation.
 * A record compares all its fields, which the duplicate-save guard relies on.
 */
public record BillInput(String name, RoomType room, BigDecimal nightlyRate, int nights,
                        BigDecimal extras, BigDecimal paid, String notes) { }
