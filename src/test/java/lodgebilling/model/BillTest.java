package lodgebilling.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The billing maths: room charge, 5-night discount, extras, balance. */
class BillTest {

    private static Bill bill(RoomType room, int nights, String extras, String paid) {
        return new Bill("R001", "John Phiri", room, nights,
                new BigDecimal(extras), new BigDecimal(paid), "");
    }

    @Test
    void shortStayGetsNoDiscount() {
        Bill b = bill(RoomType.STANDARD, 4, "0", "0");
        assertEquals(new BigDecimal("1400.00"), b.getRoomCharge());
        assertEquals(new BigDecimal("0.00"), b.getDiscount());
        assertEquals(new BigDecimal("1400.00"), b.getTotal());
    }

    @Test
    void fiveNightsGetsFivePercentOffRoomOnly() {
        // Deluxe 550 x 5 = 2750; 5% = 137.50; extras 200 are not discounted
        Bill b = bill(RoomType.DELUXE, 5, "200", "1000");
        assertEquals(new BigDecimal("2750.00"), b.getRoomCharge());
        assertEquals(new BigDecimal("137.50"), b.getDiscount());
        assertEquals(new BigDecimal("2812.50"), b.getTotal());
        assertEquals(new BigDecimal("1812.50"), b.getBalance());
    }

    @Test
    void paidInFullLeavesZeroBalance() {
        Bill b = bill(RoomType.FAMILY, 1, "0", "750");
        assertEquals(0, b.getBalance().signum());
    }
}
