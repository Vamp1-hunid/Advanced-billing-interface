package lodgebilling.controller;

import javafx.stage.Stage;
import lodgebilling.BillDao;
import lodgebilling.model.Bill;
import lodgebilling.model.BillInput;
import lodgebilling.model.BillValidator;
import lodgebilling.model.BillingModel;
import lodgebilling.model.FormField;
import lodgebilling.model.Money;
import lodgebilling.model.StatusMessage;
import lodgebilling.model.ValidationException;
import lodgebilling.view.MainView;

import java.util.Optional;

/**
 * The C in MVC: turns what the user does into changes to the model.
 * It reads the form from the view, checks it with the validator, updates the
 * model, and tells the view what to do next (focus, dialogs). It never builds
 * widgets, and it holds no data of its own.
 */
public class BillingController implements BillingActions {

    private final BillingModel model;
    private final BillDao dao;
    private final BillValidator validator = new BillValidator();
    private final Stage stage;
    private final MainView view;
    private final Runnable onLogout;

    public BillingController(BillingModel model, BillDao dao, Stage stage, Runnable onLogout) {
        this.model = model;
        this.dao = dao;
        this.stage = stage;
        this.onLogout = onLogout;
        this.view = new MainView(model, dao.findAll(), this, stage);
    }

    public MainView view() {
        return view;
    }

    /**
     * Once the user changes the input an error is about, the error is out of date,
     * so remove it. An error not tied to one input (e.g. "already saved") goes on any edit.
     */
    @Override
    public void formEdited(FormField field) {
        StatusMessage status = model.statusProperty().get();
        if (status.isError() && (status.field() == null || status.field() == field)) {
            model.clearStatus();
        }
    }

    /** Shows the working without saving. */
    @Override
    public void calculate() {
        readForm().ifPresent(input -> {
            Bill b = Bill.of("", input);
            model.setStatus(StatusMessage.info("Room " + Money.format(b.getRoomCharge())
                    + " − discount " + Money.format(b.getDiscount())
                    + " + extras " + Money.format(b.getExtras())
                    + " = total " + Money.format(b.getTotal())
                    + ". Balance: " + Money.format(b.getBalance()) + "."));
        });
    }

    @Override
    public void saveBill() {
        Optional<BillInput> read = readForm();
        if (read.isEmpty()) {
            return;
        }
        BillInput input = read.get();

        Bill existing = dao.findByGuest(input.name());
        if (existing != null) {
            model.setStatus(StatusMessage.error("A bill for " + existing.getGuestName()
                    + " is already saved as " + existing.getBillId() + ".", FormField.NAME));
            view.focusField(FormField.NAME);
            return;
        }

        Bill bill = dao.save(input);
        view.clearForm();
        model.setStatus(StatusMessage.success(
                "Saved " + bill.getBillId() + ". Balance: " + Money.format(bill.getBalance())));
        view.scrollToBill(bill);
    }

    @Override
    public void newGuest() {
        view.clearForm();
        view.clearSelection();
        model.setStatus(StatusMessage.info("Ready for a new guest."));
        view.focusField(FormField.NAME);
    }

    @Override
    public void viewReceipt() {
        Bill selected = view.selectedBill();
        if (selected == null) {
            model.setStatus(StatusMessage.error("Select a bill to view its receipt."));
            view.focusTable();
            return;
        }
        view.showReceipt(selected);
    }

    @Override
    public void printReceipt() {
        Bill selected = view.selectedBill();
        if (selected == null) {
            model.setStatus(StatusMessage.error("Select a bill to print its receipt."));
            view.focusTable();
            return;
        }
        printReceiptFor(selected);
    }

    /** Prints one bill's receipt; used by the toolbar/menu and by the receipt window's own button. */
    @Override
    public void printReceiptFor(Bill bill) {
        if (view.printReceipt(bill)) {
            model.setStatus(StatusMessage.success("Receipt " + bill.getBillId() + " sent to the printer."));
        }
    }

    @Override
    public void deleteSelected() {
        Bill selected = view.selectedBill();
        if (selected == null) {
            model.setStatus(StatusMessage.error("Select a bill to delete."));
            view.focusTable();
            return;
        }

        if (!view.confirmDelete(selected)) {
            model.setStatus(StatusMessage.info("Deletion cancelled."));
            return;
        }
        dao.delete(selected);
        view.clearSelection();
        model.setStatus(StatusMessage.success("Bill " + selected.getBillId() + " deleted."));
    }

    @Override
    public void toggleTheme() {
        model.toggleTheme();
    }

    @Override
    public void showAbout() {
        view.showAbout();
    }

    @Override
    public void logout() {
        onLogout.run();
    }

    @Override
    public void exit() {
        stage.close();
    }

    /** Reads and validates the form. On a problem, shows the message and focuses the field. */
    private Optional<BillInput> readForm() {
        try {
            return Optional.of(validator.validate(view.readForm()));
        } catch (ValidationException e) {
            model.setStatus(StatusMessage.error(e.getMessage(), e.field()));
            view.focusField(e.field());
            return Optional.empty();
        }
    }
}
