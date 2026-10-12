package lodgebilling.view;

import javafx.beans.binding.Bindings;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;
import lodgebilling.controller.BillingActions;
import lodgebilling.model.BillingModel;
import lodgebilling.model.Theme;

/** The File, View and Help menus, with their keyboard shortcuts. */
public class AppMenuBar extends MenuBar {

    public AppMenuBar(BillingActions actions, BillingModel model) {
        MenuItem newGuest = item("_New guest", "Shortcut+N", actions::newGuest);
        MenuItem receipt = item("View _receipt", "Shortcut+R", actions::viewReceipt);
        MenuItem print = item("_Print receipt", "Shortcut+P", actions::printReceipt);
        MenuItem delete = item("_Delete selected bill", "Shortcut+D", actions::deleteSelected);
        MenuItem logout = item("_Log out", "Shortcut+L", actions::logout);
        MenuItem close = item("_Close", "Shortcut+Q", actions::exit);

        Menu file = new Menu("_File");
        file.getItems().addAll(newGuest, receipt, print, delete, new SeparatorMenuItem(), logout, close);

        MenuItem themeItem = item("", "Shortcut+T", actions::toggleTheme);
        themeItem.textProperty().bind(Bindings.when(model.themeProperty().isEqualTo(Theme.LIGHT))
                .then("Switch to _dark mode").otherwise("Switch to _light mode"));
        Menu view = new Menu("_View");
        view.getItems().add(themeItem);

        Menu help = new Menu("_Help");
        help.getItems().add(item("_About", null, actions::showAbout));

        getMenus().addAll(file, view, help);
    }

    private static MenuItem item(String text, String shortcut, Runnable action) {
        MenuItem item = new MenuItem(text);
        if (shortcut != null) {
            item.setAccelerator(KeyCombination.keyCombination(shortcut));
        }
        item.setOnAction(e -> action.run());
        return item;
    }
}
