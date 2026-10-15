package lodgebilling.model;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/**
 * Today's price per night for each room type, plus the VAT % applied to stays.
 * Loaded from the database and changed only by an admin.
 */
public final class RateCard {

    private final Map<RoomType, BigDecimal> rates = new EnumMap<>(RoomType.class);
    private BigDecimal vatPercent;

    public RateCard(Map<RoomType, BigDecimal> rates, BigDecimal vatPercent) {
        for (RoomType t : RoomType.values()) {
            this.rates.put(t, Money.scale(rates.getOrDefault(t, t.getNightlyRate())));
        }
        this.vatPercent = vatPercent;
    }

    /** The original fixed prices and no VAT; used by tests and before the database is read. */
    public static RateCard defaults() {
        return new RateCard(Map.of(), BigDecimal.ZERO);
    }

    public BigDecimal rateFor(RoomType type) {
        return rates.get(type);
    }

    public BigDecimal vatPercent() {
        return vatPercent;
    }

    void set(RoomType type, BigDecimal rate) {
        rates.put(type, Money.scale(rate));
    }

    void setVatPercent(BigDecimal vat) {
        this.vatPercent = vat;
    }

    /** Replaces every value at once (after an admin saves the rates dialog). */
    public void replaceWith(RateCard other) {
        for (RoomType t : RoomType.values()) {
            set(t, other.rateFor(t));
        }
        setVatPercent(other.vatPercent());
    }
}
