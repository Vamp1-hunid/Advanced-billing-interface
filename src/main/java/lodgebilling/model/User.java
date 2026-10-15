package lodgebilling.model;

import lodgebilling.security.Role;

/**
 * A staff or admin account as the app sees it. The password hash is deliberately
 * not part of this record, so it can never end up in a table or a log by accident.
 */
public record User(long id, String username, String fullName, Role role,
                   boolean active, boolean mustChangePassword) {

    /** Getters in JavaBean style, for PropertyValueFactory in the users table. */
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public Role getRole()       { return role; }
    public String getStatus()   { return active ? "Active" : "Disabled"; }
}
