package lodgebilling.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lodgebilling.data.BillRepository;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * The M in MVC: the app's observable state. It knows nothing about screens.
 * Views observe it (the receipts list, the status message, the theme) and the
 * controller changes it in response to what the user does.
 *
 * Receipts are written through to the database, so they survive closing the app.
 */
public class BillingModel {

    private static final StatusMessage READY =
            StatusMessage.info("Enter the guest's details, then calculate or save the receipt.");

    private final BillRepository repository;
    private final Supplier<String> currentUser;

    private final ObservableList<Bill> bills = FXCollections.observableArrayList();
    private final ObjectProperty<StatusMessage> status = new SimpleObjectProperty<>(READY);
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.LIGHT);

    // The form stays filled after saving, so remember what was last saved
    // to stop the same receipt being saved twice
    private BillInput lastSavedInput;
    private String lastSavedReceiptNumber;

    /** @param currentUser who is signed in, stored on each receipt as "By" */
    public BillingModel(BillRepository repository, Supplier<String> currentUser) {
        this.repository = repository;
        this.currentUser = currentUser;
        bills.setAll(repository.findAll());
    }

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

    // ---------- Receipt operations ----------

    /** Stores and returns a new receipt; the database gives it the next number (R001, R002, ...). */
    public Bill save(BillInput input) {
        Bill bill = repository.insert(input, LocalDateTime.now(), currentUser.get());
        bills.add(bill);
        lastSavedInput = input;
        lastSavedReceiptNumber = bill.getReceiptNumber();
        return bill;
    }

    public void delete(Bill bill) {
        repository.delete(bill.getReceiptNumber());
        bills.remove(bill);
        // If the receipt still in the form was deleted, allow it to be saved again
        if (bill.getReceiptNumber().equals(lastSavedReceiptNumber)) {
            forgetLastSaved();
        }
    }

    /** True if this is exactly what was just saved (records compare all their fields). */
    public boolean isRepeatOfLastSave(BillInput input) {
        return input.equals(lastSavedInput);
    }

    public String lastSavedReceiptNumber() {
        return lastSavedReceiptNumber;
    }

    public void forgetLastSaved() {
        lastSavedInput = null;
        lastSavedReceiptNumber = null;
    }
}
