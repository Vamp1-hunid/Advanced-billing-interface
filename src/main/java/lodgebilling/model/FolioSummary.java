package lodgebilling.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * The worked-out bill for one stay, overall and split per payer.
 * Built by {@link FolioCalculator}; nothing here is stored.
 */
public record FolioSummary(BigDecimal roomCharge, BigDecimal discount, BigDecimal extras,
                           BigDecimal subtotal, BigDecimal vatPercent, BigDecimal vat,
                           BigDecimal total, BigDecimal paid, BigDecimal balance,
                           List<PayerLine> payers) {

    /** What one payer (guest or company) owes. */
    public record PayerLine(Payer payer, String name, BigDecimal subtotal, BigDecimal vat,
                            BigDecimal total, BigDecimal paid, BigDecimal balance) { }

    public boolean settled() {
        return balance.signum() <= 0;
    }
}
