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

    private final String billId;
    private final String guestName;
    private final RoomType roomType;
    private final int nights;
    private final BigDecimal extras;
    private final BigDecimal paid;
    private final String notes;

    private final LocalDateTime issuedAt = LocalDateTime.now();

    private final BigDecimal roomCharge;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final BigDecimal balance;

    public Bill(String billId, String guestName, RoomType roomType, int nights,
                BigDecimal extras, BigDecimal paid, String notes) {
        this.billId = billId;
        this.guestName = guestName;
        this.roomType = roomType;
        this.nights = nights;
        this.extras = Money.scale(extras);
        this.paid = Money.scale(paid);
        this.notes = notes;

        // Room charge = price per night x nights
        this.roomCharge = Money.scale(roomType.getNightlyRate().multiply(BigDecimal.valueOf(nights)));

        // 5 nights or more: 5% off the room charge (extras get no discount)
        this.discount = nights >= DISCOUNT_MIN_NIGHTS
                ? Money.scale(roomCharge.multiply(DISCOUNT_RATE))
                : Money.scale(BigDecimal.ZERO);

        // No tax
        this.total = roomCharge.subtract(discount).add(this.extras);
        this.balance = total.subtract(this.paid);
    }

    /** Builds a bill from validated form values. Use an empty id for an unsaved preview. */
    public static Bill of(String billId, BillInput in) {
        return new Bill(billId, in.name(), in.room(), in.nights(), in.extras(), in.paid(), in.notes());
    }

    public String getBillId()         { return billId; }
    public String getGuestName()      { return guestName; }
    public RoomType getRoomType()     { return roomType; }
    public int getNights()            { return nights; }
    public BigDecimal getExtras()     { return extras; }
    public BigDecimal getPaid()       { return paid; }
    public String getNotes()          { return notes; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public BigDecimal getRoomCharge() { return roomCharge; }
    public BigDecimal getDiscount()   { return discount; }
    public BigDecimal getTotal()      { return total; }
    public BigDecimal getBalance()    { return balance; }
}
