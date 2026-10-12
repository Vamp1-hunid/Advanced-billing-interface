package lodgebilling;

import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lodgebilling.controller.BillingController;
import lodgebilling.model.AuthService;
import lodgebilling.model.BillingModel;
import lodgebilling.view.LoginView;

/** Entry point: creates the model and controller, then shows the window. */
public class LodgeBillingApp extends Application {

    private static final double PREFERRED_WIDTH = 1100;
    private static final double PREFERRED_HEIGHT = 820;
    private static final double MIN_WIDTH = 900;
    private static final double MIN_HEIGHT = 560;
    // Room left for the window's own title bar, which is not part of the scene
    private static final double TITLE_BAR_HEIGHT = 40;

    private final AuthService auth = new AuthService();
    private final BillingModel model = new BillingModel();
    private final BillDao dao = new BillDao();

    @Override
    public void start(Stage stage) {
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
        BillingController controller = new BillingController(model, dao, stage, () -> {
            auth.logout();
            showLogin(stage);
        });
        swapScene(stage, controller.view());
        controller.view().install(stage.getScene());
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
