package lodgebilling.controller;

import lodgebilling.model.Bill;
import lodgebilling.model.FormField;

/** Everything the user can ask the app to do. Views call these; the controller implements them. */
public interface BillingActions {
    void formEdited(FormField field);
    void calculate();
    void saveBill();
    void newGuest();
    void viewReceipt();
    void printReceipt();
    void printReceiptFor(Bill bill);
    void deleteSelected();
    void toggleTheme();
    void showAbout();
    void logout();
    void exit();
}
