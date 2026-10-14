package lodgebilling.view.pages;

import javafx.stage.Stage;
import lodgebilling.data.AuditLog;
import lodgebilling.data.DataException;
import lodgebilling.model.BillingModel;
import lodgebilling.model.StatusMessage;
import lodgebilling.model.Theme;
import lodgebilling.security.AccessDeniedException;
import lodgebilling.security.Permission;
import lodgebilling.security.Session;
import lodgebilling.service.LodgeService;
import lodgebilling.service.UserFacingException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * What every page needs: the window, the signed-in user, the services, and one
 * way to report success or failure (the shared status bar).
 */
public class PageContext {

    private static final Logger LOG = Logger.getLogger(PageContext.class.getName());

    /** An action that may be refused with a message. */
    @FunctionalInterface
    public interface Action {
        void run() throws UserFacingException;
    }

    private final Stage owner;
    private final BillingModel model;
    private final LodgeService lodge;
    private final AuditLog audit;
    private Runnable refreshCurrentPage = () -> { };

    public PageContext(Stage owner, BillingModel model, LodgeService lodge, AuditLog audit) {
        this.owner = owner;
        this.model = model;
        this.lodge = lodge;
        this.audit = audit;
    }

    public Stage owner()          { return owner; }
    public LodgeService lodge()   { return lodge; }
    public AuditLog audit()       { return audit; }
    public Session session()      { return model.session(); }
    public Theme theme()          { return model.themeProperty().get(); }
    public boolean can(Permission p) { return model.session().has(p); }

    public void setRefreshCurrentPage(Runnable r) {
        this.refreshCurrentPage = r;
    }

    public void refreshCurrentPage() {
        refreshCurrentPage.run();
    }

    public void success(String text) { model.setStatus(StatusMessage.success(text)); }
    public void info(String text)    { model.setStatus(StatusMessage.info(text)); }
    public void error(String text)   { model.setStatus(StatusMessage.error(text)); }

    /** Runs an action; a refusal or database problem is shown in the status bar instead of crashing. */
    public boolean attempt(Action action) {
        try {
            action.run();
            return true;
        } catch (UserFacingException | AccessDeniedException e) {
            error(e.getMessage());
        } catch (DataException e) {
            LOG.log(Level.SEVERE, "Database problem", e);
            error("Database problem: " + e.getMessage());
        }
        return false;
    }
}
