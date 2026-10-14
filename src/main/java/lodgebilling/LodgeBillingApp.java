package lodgebilling;

import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import lodgebilling.controller.BillingController;
import lodgebilling.model.AuthService;
import lodgebilling.model.BillingModel;
import lodgebilling.data.AuditLog;
import lodgebilling.data.BillRepository;
import lodgebilling.data.DataException;
import lodgebilling.data.Database;
import lodgebilling.data.LegacyReceiptImporter;
import lodgebilling.view.LoginView;

import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point: opens the database, creates the model and controller, then shows the window.
 * Start it from {@link Launcher} when running inside IntelliJ.
 */
public class LodgeBillingApp extends Application {

    private static final Logger LOG = Logger.getLogger(LodgeBillingApp.class.getName());

    private static final double PREFERRED_WIDTH = 1100;
    private static final double PREFERRED_HEIGHT = 820;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 560;
    // Room left for the window's own title bar, which is not part of the scene
    private static final double TITLE_BAR_HEIGHT = 40;

    private final AuthService auth = new AuthService();
    private Database database;
    private BillingModel model;

    @Override
    public void start(Stage stage) {
        try {
            database = Database.open(Database.defaultPath());
        } catch (DataException e) {
            LOG.log(Level.SEVERE, "Could not open the database", e);
            Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage() + "\n\n" + e.getCause().getMessage());
            alert.setHeaderText("Lodge Billing cannot start");
            alert.showAndWait();
            return;
        }
        // Receipts saved by the earlier version (receipts.tsv) are copied in once
        AuditLog audit = new AuditLog(database, Clock.systemUTC());
        LegacyReceiptImporter.importOnce(database, audit, LegacyReceiptImporter.defaultFile());
        model = new BillingModel(new BillRepository(database), audit, auth::currentUser);

        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setTitle("Lodge Billing");
        showLogin(stage);
        stage.show();
    }

    private void showLogin(Stage stage) {
        LoginView login = new LoginView(auth, model, () -> showMain(stage));
        swapScene(stage, login);
        login.install(stage.getScene());
    }

    private void showMain(Stage stage) {
        BillingController controller = new BillingController(model, stage, () -> {
            auth.logout();
            showLogin(stage);
        });
        swapScene(stage, controller.view());
        controller.view().install(stage.getScene());

        // The billing window fades in after the login card has faded out
        controller.view().setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), controller.view());
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    @Override
    public void stop() {
        if (database != null) {
            database.close();
        }
    }

    /** Puts a new screen in the window with a fresh scene, keeping the size if it is already open. */
    private void swapScene(Stage stage, Parent root) {
        double width;
        double height;
        if (stage.getScene() != null) {
            width = stage.getScene().getWidth();
            height = stage.getScene().getHeight();
        } else {
            // Open at a comfortable size, but never bigger than the screen (minus the taskbar)
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            width = Math.min(PREFERRED_WIDTH, screen.getWidth());
            height = Math.min(PREFERRED_HEIGHT, screen.getHeight() - TITLE_BAR_HEIGHT);
        }
        stage.setScene(new Scene(root, width, height));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
