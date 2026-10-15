package lodgebilling.view;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import lodgebilling.controller.BillingActions;
import lodgebilling.model.BillingModel;
import lodgebilling.security.Permission;
import lodgebilling.security.Session;

/** Buttons for the Receipts page; Delete appears only for roles that may delete. */
public class AppToolBar extends ToolBar {

    private final Button receiptButton = new Button("View receipt");
    private final Button printButton = new Button("Print receipt");
    private final Button deleteButton = new Button("Delete selected receipt");

    public AppToolBar(BillingActions actions, BillingModel model) {
        Session session = model.session();

        Button newGuestButton = new Button("New guest");
        newGuestButton.setOnAction(e -> actions.newGuest());
        receiptButton.setOnAction(e -> actions.viewReceipt());
        printButton.setOnAction(e -> actions.printReceipt());
        deleteButton.setOnAction(e -> actions.deleteSelected());
        deleteButton.getStyleClass().add("danger");

        getItems().addAll(newGuestButton, receiptButton, printButton);
        if (session.has(Permission.DELETE_BILL)) {
            getItems().add(deleteButton);
        }
    }

    /** True for the buttons that act on the selected table row. */
    public boolean actsOnSelection(Node node) {
        return node == receiptButton || node == printButton || node == deleteButton;
    }
}
