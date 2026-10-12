package lodgebilling;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lodgebilling.model.Bill;
import lodgebilling.model.BillInput;

/**
 * Data access: stores the bills in memory and hands them out. It hides how
 * bills are kept, so the model and controller never touch the storage directly.
 */
public class BillDao {

    private final ObservableList<Bill> bills = FXCollections.observableArrayList();

    private int nextReceiptNumber = 1;

    /** Every saved bill; the list is observable so views can follow changes. */
    public ObservableList<Bill> findAll() {
        return bills;
    }

    /** Creates, stores and returns a new bill with the next number (R001, R002, ...). */
    public Bill save(BillInput input) {
        Bill bill = Bill.of(String.format("R%03d", nextReceiptNumber++), input);
        bills.add(bill);
        return bill;
    }

    public void delete(Bill bill) {
        bills.remove(bill);
    }

    /** The saved bill for this guest name (ignoring case and extra spaces), or null if none. */
    public Bill findByGuest(String name) {
        String wanted = normalise(name);
        for (Bill b : bills) {
            if (normalise(b.getGuestName()).equals(wanted)) {
                return b;
            }
        }
        return null;
    }

    private static String normalise(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
