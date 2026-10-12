package lodgebilling.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** The title bar at the top of the window, kept to one line to leave room for the bills. */
public class AppHeader extends HBox {

    public AppHeader() {
        super(14);
        Label title = new Label("Lodge Billing");
        title.getStyleClass().add("app-title");
        setAlignment(Pos.BASELINE_LEFT);
        getChildren().add(title);
        getStyleClass().add("header");
    }
}
