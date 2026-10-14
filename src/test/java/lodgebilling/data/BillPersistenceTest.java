package lodgebilling.data;

import lodgebilling.model.Bill;
import lodgebilling.model.BillInput;
import lodgebilling.model.BillingModel;
import lodgebilling.model.RoomType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillPersistenceTest {

    private Database db;
    private BillRepository repo;

    @BeforeEach
    void setUp() {
        db = Database.inMemory();
        repo = new BillRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private static BillInput input(String name) {
        return new BillInput(name, RoomType.DELUXE, new BigDecimal("550"), 5, new BigDecimal("200"), new BigDecimal("1000.50"), "late checkout");
    }

    private BillingModel model() {
        return new BillingModel(repo, () -> "mary");
    }

    @Test
    void savedBillsComeBackExactly() {
        Bill saved = model().save(input("John Phiri"));

        // A fresh model reads from the database, as after restarting the app
        Bill loaded = model().bills().get(0);
        assertEquals(saved.getReceiptNumber(), loaded.getReceiptNumber());
        assertEquals("mary", loaded.getCreatedBy());
        assertEquals(new BigDecimal("1000.50"), loaded.getPaid());   // no floating-point drift
        assertEquals(new BigDecimal("2812.50"), loaded.getTotal());
        assertEquals("late checkout", loaded.getNotes());
    }

    @Test
    void deleteRemovesFromDatabaseAndNumbersAreNotReused() {
        BillingModel model = model();
        model.save(input("Guest One"));
        Bill second = model.save(input("Guest Two"));
        model.delete(second);
        assertEquals(1, repo.findAll().size());

        Bill third = model.save(input("Guest Three"));
        assertEquals("R003", third.getReceiptNumber());   // R002 is gone for good, not handed out again
    }

    @Test
    void guestNameWithQuoteIsStoredSafely() {
        model().save(input("Mary O'Neil"));
        assertEquals("Mary O'Neil", repo.findAll().get(0).getGuestName());
    }
}
