package lodgebilling.view;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lodgebilling.model.AuthService;
import lodgebilling.model.BillingModel;
import lodgebilling.model.Theme;

/** The sign-in screen. Calls onSuccess once the username and password are accepted. */
public class LoginView extends StackPane {

    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label error = new Label();
    private ChangeListener<Theme> themeListener;

    public LoginView(AuthService auth, BillingModel model, Runnable onSuccess) {
        Label title = new Label("Lodge Billing");
        title.getStyleClass().add("app-title");

        username.setPromptText("Username");
        password.setPromptText("Password");

        error.getStyleClass().addAll("status", "error");
        error.setWrapText(true);
        error.setMaxWidth(Double.MAX_VALUE);
        error.setVisible(false);
        error.setManaged(false);

        Button signIn = new Button("Sign in");
        signIn.setDefaultButton(true);
        signIn.setMaxWidth(Double.MAX_VALUE);
        signIn.setOnAction(e -> {
            if (auth.login(username.getText(), password.getText().toCharArray())) {
                password.clear();
                hideError();
                model.themeProperty().removeListener(themeListener);
                onSuccess.run();
            } else {
                password.clear();
                showError("Wrong username or password.");
                password.requestFocus();
            }
        });
        username.textProperty().addListener((obs, old, v) -> hideError());
        password.textProperty().addListener((obs, old, v) -> hideError());

        VBox card = new VBox(12, title, username, password, error, signIn);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(28));
        card.setMaxWidth(360);
        card.setMaxHeight(USE_PREF_SIZE);

        ThemeToggle themeToggle = new ThemeToggle(model.themeProperty(), model::toggleTheme);
        themeToggle.setFocusTraversable(false);
        StackPane.setAlignment(themeToggle, Pos.TOP_RIGHT);

        // Follow the model's theme, the same way the main window does
        themeListener = (obs, old, theme) -> Styles.apply(theme, this);
        Styles.apply(model.themeProperty().get(), this);
        model.themeProperty().addListener(themeListener);

        setPadding(new Insets(24));
        getChildren().addAll(card, themeToggle);
    }

    /** Call once the scene exists. */
    public void install(javafx.scene.Scene scene) {
        scene.getStylesheets().add(Styles.stylesheet());
        Platform.runLater(username::requestFocus);
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
    }

    private void hideError() {
        error.setVisible(false);
        error.setManaged(false);
    }
}
