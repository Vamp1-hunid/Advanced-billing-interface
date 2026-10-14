package lodgebilling.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Append-only record of who did what and when. Nothing in the app edits or deletes it. */
public class AuditLog {

    public enum Action {
        SETUP, LOGIN, LOGIN_FAILED, LOGOUT, ACCOUNT_LOCKED,
        BILL_SAVED, BILL_DELETED,
        USER_CREATED, USER_ENABLED, USER_DISABLED, ROLE_CHANGED, PASSWORD_RESET, PASSWORD_CHANGED,
        RECEIPTS_IMPORTED,
        BOOKING_CREATED, BOOKING_UPDATED, BOOKING_CANCELLED, CHECK_IN, CHECK_OUT, FORCED_CHECKOUT,
        CHARGE_POSTED, PAYMENT_RECORDED, ENTRY_VOIDED,
        ROOM_ADDED, ROOM_STATUS, RATES_CHANGED, REPORT_EXPORTED
    }

    /** One row of the log, with JavaBean getters for the table. */
    public record Entry(Instant at, String username, Action action, String details) {
        public Instant getAt()       { return at; }
        public String getUsername()  { return username; }
        public Action getAction()    { return action; }
        public String getDetails()   { return details; }
    }

    private static final Logger LOG = Logger.getLogger(AuditLog.class.getName());

    private final Connection db;
    private final Clock clock;

    public AuditLog(Database database, Clock clock) {
        this.db = database.connection();
        this.clock = clock;
    }

    /** Writes one entry. A failure here is logged but never stops the user's work. */
    public void record(String username, Action action, String details) {
        String sql = "INSERT INTO audit_log (at, username, action, details) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, Instant.now(clock).toString());
            ps.setString(2, username == null ? "" : username);
            ps.setString(3, action.name());
            ps.setString(4, details == null ? "" : details);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.warning("Could not write audit entry " + action + ": " + e.getMessage());
        }
    }

    /** The newest entries first. */
    public List<Entry> latest(int limit) {
        String sql = "SELECT at, username, action, details FROM audit_log ORDER BY id DESC LIMIT ?";
        List<Entry> entries = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new Entry(Instant.parse(rs.getString("at")), rs.getString("username"),
                            Action.valueOf(rs.getString("action")), rs.getString("details")));
                }
            }
            return entries;
        } catch (SQLException e) {
            throw new DataException("Could not read the audit log", e);
        }
    }
}
