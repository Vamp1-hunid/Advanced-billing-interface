package lodgebilling.view;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import lodgebilling.controller.BillingActions;
import lodgebilling.model.BillingModel;

/** Quick-access buttons under the menu, with the light/dark switch on the right. */
public class AppToolBar extends ToolBar {

    private final Button receiptButton = new Button("View receipt");
    private final Button deleteButton = new Button("Delete selected receipt");

    public AppToolBar(BillingActions actions, BillingModel model) {
        Button newGuestButton = new Button("New guest");
        newGuestButton.setOnAction(e -> actions.newGuest());
        receiptButton.setOnAction(e -> actions.viewReceipt());
        deleteButton.setOnAction(e -> actions.deleteSelected());
        deleteButton.getStyleClass().add("danger");

        ThemeToggle themeToggle = new ThemeToggle(model.themeProperty(), actions::toggleTheme);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        getItems().addAll(newGuestButton, receiptButton, deleteButton, spacer, themeToggle);
    }

    /** True for the buttons that act on the selected table row. */
    public boolean actsOnSelection(Node node) {
        return node == receiptButton || node == deleteButton;
    }
}
