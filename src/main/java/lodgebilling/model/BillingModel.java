package lodgebilling.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * The M in MVC: the app's observable state. It knows nothing about screens.
 * Views observe it (the status message, the theme) and the
 * controller changes it in response to what the user does.
 */
public class BillingModel {

    private static final StatusMessage READY =
            StatusMessage.info("Enter the guest's details, then calculate or save the bill.");

    private final ObjectProperty<StatusMessage> status = new SimpleObjectProperty<>(READY);
    private final ObjectProperty<Theme> theme = new SimpleObjectProperty<>(Theme.LIGHT);

    // ---------- Observable state ----------

    public ObjectProperty<StatusMessage> statusProperty() {
        return status;
    }

    public void setStatus(StatusMessage message) {
        status.set(message);
    }

    /** Back to the opening prompt, e.g. once the user starts fixing an error. */
    public void clearStatus() {
        status.set(READY);
    }

    public ObjectProperty<Theme> themeProperty() {
        return theme;
    }

    public void toggleTheme() {
        theme.set(theme.get().toggled());
    }

}
