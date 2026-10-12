package lodgebilling.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lodgebilling.model.Bill;
import lodgebilling.model.Money;
import lodgebilling.model.RoomType;

/** Card listing the room prices and the billing rules. */
public class PricePanel extends VBox {

    public PricePanel() {
        super(6);
        Label heading = new Label("Room prices");
        heading.getStyleClass().add("card-title");
        getChildren().add(heading);

        for (RoomType room : RoomType.values()) {
            getChildren().add(priceRow(room));
        }

        Label rules = new Label(Bill.DISCOUNT_MIN_NIGHTS + " nights or more: 5% off the room charge.");
        rules.setWrapText(true);
        rules.getStyleClass().add("hint");
        VBox.setMargin(rules, new Insets(14, 0, 0, 0));
        getChildren().add(rules);

        getStyleClass().add("card");
        setPrefWidth(280);
        setMinWidth(240);
    }

    private static HBox priceRow(RoomType room) {
        Label name = new Label(room.getLabel());
        name.getStyleClass().add("price-row");
        Label amount = new Label(Money.format(room.getNightlyRate()) + " / night");
        amount.getStyleClass().add("price-amount");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(name, spacer, amount);
    }
}
