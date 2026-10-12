package lodgebilling.view;

import javafx.scene.Parent;
import lodgebilling.model.Theme;

/** Where the stylesheet lives and how a theme is applied to a window. */
public final class Styles {

    private static final String STYLESHEET = "/lodge.css";
    private static final String DARK_CLASS = "dark";

    private Styles() { }

    public static String stylesheet() {
        return Styles.class.getResource(STYLESHEET).toExternalForm();
    }

    /** Adds or removes the "dark" style class that switches the CSS colour palette. */
    public static void apply(Theme theme, Parent root) {
        if (theme == Theme.DARK) {
            if (!root.getStyleClass().contains(DARK_CLASS)) {
                root.getStyleClass().add(DARK_CLASS);
            }
        } else {
            root.getStyleClass().remove(DARK_CLASS);
        }
    }
}
