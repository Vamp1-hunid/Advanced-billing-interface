package lodgebilling.view;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
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
import javafx.util.Duration;
import lodgebilling.model.AuthService;
import lodgebilling.model.BillingModel;
import lodgebilling.model.Theme;

/** The sign-in screen. Calls onSuccess once the username and password are accepted. */
public class LoginView extends StackPane {

    private static final Duration EXIT_TIME = Duration.millis(450);

    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label error = new Label();
    private ChangeListener<Theme> themeListener;
    private SlatBackground slats;
    private VBox card;
    private ThemeToggle themeToggle;

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
                playExit(onSuccess);
            } else {
                password.clear();
                showError("Wrong username or password.");
                password.requestFocus();
            }
        });
        username.textProperty().addListener((obs, old, v) -> hideError());
        password.textProperty().addListener((obs, old, v) -> hideError());

        card = new VBox(12, title, username, password, error, signIn);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(28));
        card.setMaxWidth(360);
        card.setMaxHeight(USE_PREF_SIZE);

        themeToggle = new ThemeToggle(model.themeProperty(), model::toggleTheme);
        themeToggle.setFocusTraversable(false);
        StackPane.setAlignment(themeToggle, Pos.TOP_RIGHT);

        // Follow the model's theme, the same way the main window does
        slats = new SlatBackground(model.themeProperty().get() == Theme.DARK);
        slats.setManaged(false);
        themeListener = (obs, old, theme) -> {
            Styles.apply(theme, this);
            slats.setDark(theme == Theme.DARK);
        };
        Styles.apply(model.themeProperty().get(), this);
        model.themeProperty().addListener(themeListener);

        setPadding(new Insets(24));
        getChildren().addAll(slats, card, themeToggle);
    }

    /** The card shrinks and fades away; only then does the billing window open. */
    private void playExit(Runnable next) {
        card.setDisable(true);
        themeToggle.setDisable(true);

        FadeTransition cardFade = new FadeTransition(EXIT_TIME, card);
        cardFade.setToValue(0);
        ScaleTransition cardShrink = new ScaleTransition(EXIT_TIME, card);
        cardShrink.setToX(0.9);
        cardShrink.setToY(0.9);
        FadeTransition toggleFade = new FadeTransition(EXIT_TIME, themeToggle);
        toggleFade.setToValue(0);

        ParallelTransition exit = new ParallelTransition(cardFade, cardShrink, toggleFade);
        exit.setInterpolator(Interpolator.EASE_IN);
        exit.setOnFinished(e -> {
            slats.stop();
            next.run();
        });
        exit.play();
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // the backdrop ignores the padding and fills the whole window
        slats.resizeRelocate(0, 0, getWidth(), getHeight());
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
