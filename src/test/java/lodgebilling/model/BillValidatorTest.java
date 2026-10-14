package lodgebilling.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Every validation rule, plus the field each error points the user to. */
class BillValidatorTest {

    private final BillValidator validator = new BillValidator();

    private static BillFormData form(String name, RoomType room, String nights, String extras, String paid) {
        return new BillFormData(name, room, nights, extras, paid, "");
    }

    private FormField errorField(BillFormData data) {
        return assertThrows(ValidationException.class, () -> validator.validate(data)).field();
    }

    @Test
    void validFormIsCleanedUp() throws ValidationException {
        BillInput in = validator.validate(form("  Mary   O'Neil-Banda ", RoomType.STANDARD, " 3 ", "", "100.5"));
        assertEquals("Mary O'Neil-Banda", in.name());
        assertEquals(3, in.nights());
        assertEquals(BigDecimal.ZERO, in.extras());      // blank means 0
        assertEquals(new BigDecimal("100.5"), in.paid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A", "John2", "John  -Phiri", "-John"})
    void badNamesPointAtName(String name) {
        assertEquals(FormField.NAME, errorField(form(name, RoomType.STANDARD, "1", "", "")));
    }

    @Test
    void missingRoomPointsAtRoom() {
        assertEquals(FormField.ROOM, errorField(form("John", null, "1", "", "")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "31", "-1", "2.5", "abc"})
    void badNightsPointAtNights(String nights) {
        assertEquals(FormField.NIGHTS, errorField(form("John", RoomType.STANDARD, nights, "", "")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5", "1,000", "10.555", "K200"})
    void badExtrasPointAtExtras(String extras) {
        assertEquals(FormField.EXTRAS, errorField(form("John", RoomType.STANDARD, "1", extras, "")));
    }

    @Test
    void overpaymentPointsAtPaid() {
        // Standard x 1 night = 350, so paying 351 is too much
        assertEquals(FormField.PAID, errorField(form("John", RoomType.STANDARD, "1", "", "351")));
    }
}
