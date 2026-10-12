package lodgebilling.model;

import java.math.BigDecimal;

/**
 * The values from the form once they have passed validation.
 */
public record BillInput(String name, RoomType room, int nights,
                        BigDecimal extras, BigDecimal paid, String notes) { }
