package lodgebilling.view;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import lodgebilling.controller.BillingActions;
import lodgebilling.model.BillingModel;
import lodgebilling.model.User;

// Lab 3 - Group 1
/** The banner across the top: title, current page, who is signed in, theme switch and log out. */
public class AppHeader extends HBox {

    private final Label pageTitle = new Label();

    public AppHeader(BillingModel model, BillingActions actions) {
        super(14);
        User user = model.session().user();
        Label stars = new Label("★★★★★");
        stars.getStyleClass().add("app-stars");
        Label title = new Label("Lodge Billing");
        title.getStyleClass().add("app-title");
        pageTitle.getStyleClass().add("app-subtitle");

        Label who = new Label(user.fullName());
        who.getStyleClass().add("user-badge");
        Label role = new Label(user.role().getLabel());
        role.getStyleClass().add("user-badge-role");

        ThemeToggle theme = new ThemeToggle(model.themeProperty(), actions::toggleTheme);
        Button logout = new Button("Log out");
        logout.setOnAction(e -> actions.logout());

        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(stars, title, pageTitle, Ui.spacer(), who, role, theme, logout);
        getStyleClass().add("header");
    }

    public void setPage(String name) {
        pageTitle.setText("/  " + name);
    }
}
