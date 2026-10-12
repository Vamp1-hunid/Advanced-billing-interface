package lodgebilling.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money helpers: every amount is a BigDecimal with 2 decimal places. */
public final class Money {

    private Money() { }

    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /** Formats an amount the way the screen shows it, e.g. "K 2812.50". */
    public static String format(BigDecimal value) {
        return "K " + scale(value).toPlainString();
    }
}
