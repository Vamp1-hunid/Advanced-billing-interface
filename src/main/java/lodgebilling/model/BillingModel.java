package lodgebilling.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * The M in MVC: everything the app remembers. It knows nothing about screens.
 * Views observe it (the bills list, the status message, the theme) and the
 * controller changes it in response to what the user does.
 */
public class BillingModel {

    private static final StatusMessage READY =
            StatusMessage.info("Enter the guest's details, then calculate or save the bill.");

    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private final ObjectProperty<StatusMessage> status = new SimpleObjectProperty<>(READY);
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.LIGHT);

    private int nextBillNumber = 1;

    // ---------- Observable state ----------

    public ObservableList<Bill> bills() {
        return bills;
    }

    public ObjectProperty<StatusMessage> statusProperty() {
        return status;
    }

    public void setStatus(StatusMessage message) {
        status.set(message);
    }

    /** Back to the opening prompt, e.g. once the user starts fixing an error. */
    public void clearStatus() {
        status.set(READY);
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    public void toggleTheme() {
        theme.set(theme.get().toggled());
    }

    // ---------- Bill operations ----------

    /** Creates, stores and returns a new bill with the next number (B001, B002, ...). */
    public Bill save(BillInput input) {
        Bill bill = Bill.of(String.format("B%03d", nextBillNumber++), input);
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
