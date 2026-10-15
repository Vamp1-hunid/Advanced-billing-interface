package lodgebilling.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One saved bill (one row in the table). Immutable: everything is worked out
 * once in the constructor. The public getters are what PropertyValueFactory reads.
 */
public class Bill {

    // ---- Billing rules ----
    public static final int DISCOUNT_MIN_NIGHTS = 5;
    public static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.05"); // 5% off the room charge

    private final String receiptNumber;
    private final String guestName;
    private final RoomType roomType;
    private final BigDecimal nightlyRate;
    private final int nights;
    private final BigDecimal extras;
    private final BigDecimal paid;
    private final String notes;
    private final LocalDateTime issuedAt;
    private final String createdBy;

    private final BigDecimal roomCharge;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final BigDecimal balance;

    /** An unsaved bill (a preview), issued now by nobody in particular. */
    public Bill(String receiptNumber, String guestName, RoomType roomType, int nights,
                BigDecimal extras, BigDecimal paid, String notes) {
        this(receiptNumber, guestName, roomType, roomType.getNightlyRate(), nights, extras, paid, notes,
                LocalDateTime.now(), "");
    }

    /**
     * @param nightlyRate the price per night when the bill was made (copied, so a
     *                    later price change does not alter an old bill)
     */
    public Bill(String receiptNumber, String guestName, RoomType roomType, BigDecimal nightlyRate, int nights,
                BigDecimal extras, BigDecimal paid, String notes,
                LocalDateTime issuedAt, String createdBy) {
        this.nightlyRate = Money.scale(nightlyRate);
        this.issuedAt = issuedAt;
        this.createdBy = createdBy;
        this.receiptNumber = receiptNumber;
        this.guestName = guestName;
        this.roomType = roomType;
        this.nights = nights;
        this.extras = Money.scale(extras);
        this.paid = Money.scale(paid);
        this.notes = notes;

        // Room charge = price per night x nights
        this.roomCharge = Money.scale(this.nightlyRate.multiply(BigDecimal.valueOf(nights)));

        // 5 nights or more: 5% off the room charge (extras get no discount)
        this.discount = nights >= DISCOUNT_MIN_NIGHTS
                ? Money.scale(roomCharge.multiply(DISCOUNT_RATE))
                : Money.scale(BigDecimal.ZERO);

        // No tax
        this.total = roomCharge.subtract(discount).add(this.extras);
        this.balance = total.subtract(this.paid);
    }

    /** Builds a bill from validated form values. Use an empty id for an unsaved preview. */
    public static Bill of(String receiptNumber, BillInput in) {
        return new Bill(receiptNumber, in.name(), in.room(), in.nightlyRate(), in.nights(), in.extras(), in.paid(),
                in.notes(), LocalDateTime.now(), "");
    }

    /** Receipt number 7 is shown as "R007". */
    public static String idFor(long number) {
        return String.format("R%03d", number);
    }

    /** "R007" back to 7. */
    public static long numberOf(String receiptNumber) {
        return Long.parseLong(receiptNumber.substring(1));
    }

    /** "5%", built from DISCOUNT_RATE so the screen can never disagree with the maths. */
    public static String discountPercentLabel() {
        return DISCOUNT_RATE.movePointRight(2).stripTrailingZeros().toPlainString() + "%";
    }

    public String getReceiptNumber()         { return receiptNumber; }
    public String getGuestName()      { return guestName; }
    public RoomType getRoomType()     { return roomType; }
    public BigDecimal getNightlyRate() { return nightlyRate; }
    public int getNights()            { return nights; }
    public BigDecimal getExtras()     { return extras; }
    public BigDecimal getPaid()       { return paid; }
    public String getNotes()          { return notes; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public String getCreatedBy()      { return createdBy; }
    public BigDecimal getRoomCharge() { return roomCharge; }
    public BigDecimal getDiscount()   { return discount; }
    public BigDecimal getTotal()      { return total; }
    public BigDecimal getBalance()    { return balance; }
}
