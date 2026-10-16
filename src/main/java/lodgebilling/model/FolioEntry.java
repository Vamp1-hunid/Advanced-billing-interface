package lodgebilling.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One line on a guest's bill. For a CHARGE, category is a {@link ChargeCategory} name;
 * for a PAYMENT or REFUND it is a {@link PaymentMethod} name. Amounts are always positive;
 * a REFUND (change handed back) counts against what was paid.
 */
public record FolioEntry(long id, long reservationId, Kind kind, String category, String description,
                         BigDecimal amount, Payer payer, LocalDateTime postedAt, String postedBy,
                         boolean voided, String voidReason) {

    public enum Kind { CHARGE, PAYMENT, REFUND }

    public boolean isCharge()  { return kind == Kind.CHARGE; }
    public boolean isPayment() { return kind == Kind.PAYMENT; }
    public boolean isRefund()  { return kind == Kind.REFUND; }

    /** Readable category, e.g. "Bar" or "Mobile money". */
    public String categoryLabel() {
        return isCharge() ? ChargeCategory.valueOf(category).toString()
                          : PaymentMethod.valueOf(category).toString();
    }
}
