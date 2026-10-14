package lodgebilling.view;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lodgebilling.controller.BillingActions;
import lodgebilling.model.Bill;
import lodgebilling.model.BillFormData;
import lodgebilling.model.BillingModel;
import lodgebilling.model.FormField;
import lodgebilling.model.RateCard;

/**
 * The V in MVC: the whole window. It lays out the smaller views, shows what the
 * model holds, and reports clicks to the controller. It makes no decisions
 * about bills itself.
 */
public class MainView extends BorderPane {

    private final BillFormView form;
    private final BillTableView table;
    private final AppToolBar toolBar;
    private final Dialogs dialogs;
    private final ScrollPane scroll;
    private final AuroraBackground aurora = new AuroraBackground();
    private final VBox top;
    private HBox formAndPrices;
    private StatusBar statusBar;
    private VBox tableCard;
    // Kept so it can be removed again at logout (the Scene outlives this view)
    private ChangeListener<Node> focusWatcher;

    public MainView(BillingModel model, BillingActions actions, Stage stage) {
        this.form = new BillFormView(actions, model.statusProperty());
        this.table = new BillTableView(model.bills(), actions::viewReceipt, actions::deleteSelected);
        this.toolBar = new AppToolBar(actions, model);
        this.dialogs = new Dialogs(stage, model, actions);

        // The aurora sits behind everything and fills the whole window
        aurora.setManaged(false);
        getChildren().add(aurora);

        top = new VBox(new AppMenuBar(actions, model), new AppHeader(model, actions), toolBar);
        setTop(top);
        scroll = new ScrollPane(buildContent(model));
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().add("main-scroll");
        setCenter(scroll);

        // Switch colours whenever the model's theme changes
        Styles.apply(model.themeProperty().get(), this);
        model.themeProperty().addListener((obs, old, theme) -> Styles.apply(theme, this));
    }

    private VBox buildContent(BillingModel model) {
        HBox.setHgrow(form, Priority.ALWAYS);
        // Prices are the fixed defaults for now; editable rates come with the rooms work
        formAndPrices = new HBox(16, form, new PricePanel(RateCard.defaults()));

        Label savedLabel = new Label("Saved receipts");
        savedLabel.getStyleClass().add("card-title");
        VBox.setVgrow(table, Priority.ALWAYS);
        tableCard = new VBox(10, savedLabel, table);
        tableCard.getStyleClass().add("card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        statusBar = new StatusBar(model.statusProperty());
        VBox content = new VBox(12, formAndPrices, statusBar, tableCard);
        content.setPadding(new Insets(16, 20, 20, 20));
        return content;
    }

    /** Call once the scene exists: animations and focus behaviour that need the whole scene. */
    public void install(Scene scene) {
        Motion.liftButtons(this);
        Motion.enter(top, formAndPrices, statusBar, tableCard);

        // Deselect the row when focus leaves the table, except when it goes to
        // a button that needs the selection (View receipt / Delete)
        focusWatcher = (obs, oldOwner, newOwner) -> {
            if (newOwner != null && newOwner != table && !toolBar.actsOnSelection(newOwner)) {
                table.clearSelection();
            }
        };
        scene.focusOwnerProperty().addListener(focusWatcher);
        // Logical initial focus: start in the guest name field, with the form
        // scrolled into view (on a small screen the page may need to scroll)
        Platform.runLater(() -> {
            form.focus(FormField.NAME);
            scroll.setVvalue(0);
        });
    }

    /** Call before the scene switches to another screen (logout). */
    public void uninstall(Scene scene) {
        aurora.stop();
        if (focusWatcher != null) {
            scene.focusOwnerProperty().removeListener(focusWatcher);
            focusWatcher = null;
        }
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // the backdrop fills the whole window, behind the bars and the cards
        aurora.resizeRelocate(0, 0, getWidth(), getHeight());
    }

    // ---------- What the controller can ask of the screen ----------

    public BillFormData readForm()          { return form.read(); }
    public void clearForm()                 { form.clear(); }
    public void focusField(FormField field) { form.focus(field); }
    public Bill selectedBill()              { return table.selectedBill(); }
    public void clearSelection()            { table.clearSelection(); }
    public void focusTable()                { table.requestFocus(); }
    public void scrollToBill(Bill bill)     { table.scrollTo(bill); }

    public void showReceipt(Bill bill)      { dialogs.showReceipt(bill); }
    public boolean printReceipt(Bill bill)  { return dialogs.printReceipt(bill); }
    public boolean confirmDelete(Bill bill) { return dialogs.confirmDelete(bill); }
    public void showAbout()                 { dialogs.showAbout(); }
    public boolean changePassword(boolean forced, ChangePasswordDialog.Change change) {
        return dialogs.changePassword(forced, change);
    }
}
