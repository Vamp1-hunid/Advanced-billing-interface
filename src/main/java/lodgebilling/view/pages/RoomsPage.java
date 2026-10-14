package lodgebilling.view.pages;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lodgebilling.model.Money;
import lodgebilling.model.Room;
import lodgebilling.model.RoomStatus;
import lodgebilling.model.RoomType;
import lodgebilling.security.Permission;
import lodgebilling.view.FormDialog;
import lodgebilling.view.Ui;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Room board for housekeeping and the front desk: one coloured tile per room.
 * Select a tile (click, or Tab + Space), then mark it clean, dirty or under maintenance.
 */
public class RoomsPage implements Page {

    private final PageContext ctx;
    private final FlowPane tiles = new FlowPane(12, 12);
    private final ToggleGroup group = new ToggleGroup();
    private final HBox counts = new HBox(8);
    private final VBox root;

    public RoomsPage(PageContext ctx) {
        this.ctx = ctx;
        HBox actions = Ui.row(
                Ui.button("Mark clean (Available)", () -> setStatus(RoomStatus.AVAILABLE)),
                Ui.button("Mark dirty", () -> setStatus(RoomStatus.DIRTY)),
                Ui.button("Maintenance", () -> setStatus(RoomStatus.MAINTENANCE)));
        if (ctx.can(Permission.MANAGE_ROOMS)) {
            actions.getChildren().addAll(Ui.spacer(), Ui.button("Add room", this::addRoom));
        }
        ScrollPane scroll = new ScrollPane(tiles);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("main-scroll");
        Label hint = new Label("Select a room, then choose its new state. A room becomes Occupied only "
                + "through check-in, and Dirty automatically at check-out.");
        hint.getStyleClass().add("hint");
        hint.setWrapText(true);
        root = new VBox(12, actions, counts, Ui.card("Rooms", hint, scroll));
        root.setPadding(new Insets(16, 20, 16, 20));
    }

    @Override
    public Node node() {
        return root;
    }

    @Override
    public void refresh() {
        Long selectedId = group.getSelectedToggle() == null ? null : (Long) group.getSelectedToggle().getUserData();
        ctx.attempt(() -> {
            List<Room> rooms = ctx.lodge().rooms();
            Map<RoomStatus, Integer> byStatus = new EnumMap<>(RoomStatus.class);
            for (RoomStatus s : RoomStatus.values()) byStatus.put(s, 0);
            tiles.getChildren().clear();
            group.getToggles().clear();
            for (Room room : rooms) {
                byStatus.merge(room.status(), 1, Integer::sum);
                tiles.getChildren().add(tile(room, selectedId));
            }
            counts.getChildren().clear();
            for (RoomStatus s : RoomStatus.values()) {
                counts.getChildren().add(Ui.chip(s + ": " + byStatus.get(s), s.styleClass()));
            }
        });
    }

    private Node tile(Room room, Long selectedId) {
        Label number = new Label(room.number());
        number.getStyleClass().add("tile-number");
        Label type = new Label(room.type() + "  " + Money.format(room.type().getNightlyRate()));
        type.getStyleClass().add("tile-type");
        Label status = Ui.chip(room.status().toString(), room.status().styleClass());
        VBox box = new VBox(4, number, type, status);
        box.setAlignment(Pos.TOP_LEFT);
        ToggleButton t = new ToggleButton();
        t.setGraphic(box);
        t.setUserData(room.id());
        t.setToggleGroup(group);
        t.getStyleClass().addAll("room-tile", "tile-" + room.status().name().toLowerCase());
        t.setPrefSize(180, 120);
        if (selectedId != null && selectedId == room.id()) {
            t.setSelected(true);
        }
        return t;
    }

    private Room selectedRoom() {
        if (group.getSelectedToggle() == null) {
            ctx.error("Select a room tile first.");
            return null;
        }
        long id = (Long) group.getSelectedToggle().getUserData();
        return ctx.lodge().rooms().stream().filter(r -> r.id() == id).findFirst().orElse(null);
    }

    private void setStatus(RoomStatus status) {
        Room room = selectedRoom();
        if (room != null && ctx.attempt(() -> ctx.lodge().setRoomStatus(ctx.session(), room, status))) {
            ctx.success("Room " + room.number() + " is now " + status + ".");
        }
        refresh();
    }

    private void addRoom() {
        FormDialog form = new FormDialog(ctx.owner(), ctx.theme(), "Add room", "Add a room to the lodge",
                "Add room", "Cancel");
        TextField number = form.add("Room number", new TextField());
        ComboBox<RoomType> type = form.add("Type", new ComboBox<>(FXCollections.observableArrayList(RoomType.values())));
        type.setValue(RoomType.STANDARD);
        form.onSubmit(() -> ctx.lodge().addRoom(ctx.session(), number.getText(), type.getValue()));
        if (form.showAndWait()) {
            ctx.success("Room " + number.getText().trim() + " added.");
        }
        refresh();
    }
}
