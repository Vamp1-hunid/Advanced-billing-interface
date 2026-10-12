package lodgebilling.view;

import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import lodgebilling.model.Bill;
import lodgebilling.model.Money;
import lodgebilling.model.RoomType;

import java.math.BigDecimal;

/** The table of saved bills, bound directly to the repository's list. */
public class BillTableView extends TableView<Bill> {

    public BillTableView(ObservableList<Bill> bills, Runnable onOpen, Runnable onDelete) {
        TableColumn<Bill, String> idCol = new TableColumn<>("Receipt no.");
        idCol.setCellValueFactory(new PropertyValueFactory<>("receiptNumber"));

        TableColumn<Bill, String> guestCol = new TableColumn<>("Guest");
        guestCol.setCellValueFactory(new PropertyValueFactory<>("guestName"));

        TableColumn<Bill, RoomType> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomType"));

        TableColumn<Bill, Integer> nightsCol = new TableColumn<>("Nights");
        nightsCol.setCellValueFactory(new PropertyValueFactory<>("nights"));
        nightsCol.setStyle("-fx-alignment: CENTER;");

        getColumns().add(idCol);
        getColumns().add(guestCol);
        getColumns().add(roomCol);
        getColumns().add(nightsCol);
        getColumns().add(moneyColumn("Total", "total"));
        getColumns().add(moneyColumn("Paid", "paid"));
        getColumns().add(moneyColumn("Balance", "balance"));

        setItems(bills);
        setPlaceholder(new Label("No receipts saved yet."));
        // Columns share the full width in proportion to their preferred widths,
        // so the table fills its card with no empty strip and no scrollbar
        setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        idCol.setPrefWidth(90);
        guestCol.setPrefWidth(220);
        roomCol.setPrefWidth(120);
        nightsCol.setPrefWidth(70);
        for (int i = 4; i < getColumns().size(); i++) {
            getColumns().get(i).setPrefWidth(110);
        }
        setMinHeight(160);

        // Double-click a row to open its receipt
        setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedBill() != null) {
                onOpen.run();
            }
        });

        // Enter on the selected row opens its receipt; Space asks to delete it
        setOnKeyPressed(e -> {
            if (selectedBill() == null) {
                return;
            }
            if (e.getCode() == KeyCode.ENTER) {
                onOpen.run();
                e.consume();
            } else if (e.getCode() == KeyCode.SPACE) {
                onDelete.run();
                e.consume();
            }
        });
    }

    /** The highlighted bill, or null when nothing is selected. */
    public Bill selectedBill() {
        return getSelectionModel().getSelectedItem();
    }

    public void clearSelection() {
        getSelectionModel().clearSelection();
    }

    /** A right-aligned column that shows a BigDecimal as "K 0.00". */
    private static TableColumn<Bill, BigDecimal> moneyColumn(String title, String property) {
        TableColumn<Bill, BigDecimal> col = new TableColumn<>(title);
        col.setCellValueFactory(new PropertyValueFactory<>(property));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : Money.format(value));
            }
        });
        col.setStyle("-fx-alignment: CENTER-RIGHT;");
        return col;
    }
}
