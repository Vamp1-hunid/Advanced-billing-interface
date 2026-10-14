package lodgebilling.security;

import java.util.EnumSet;
import java.util.Set;

/** What kind of account a user has. Each role maps to a fixed set of permissions. */
public enum Role {
    /** Front desk: make and print receipts, but no deletes or user management. */
    STAFF("Front desk", EnumSet.of(Permission.CREATE_BILL, Permission.PRINT_RECEIPT)),

    /** General manager: everything. */
    ADMIN("Admin", EnumSet.allOf(Permission.class));

    private final String label;
    private final Set<Permission> permissions;

    Role(String label, Set<Permission> permissions) {
        this.label = label;
        this.permissions = permissions;
    }

    public boolean can(Permission permission) {
        return permissions.contains(permission);
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
