package lodgebilling.security;

/**
 * Things a user may be allowed to do. Code checks permissions, never roles,
 * so adding a role only means editing {@link Role}.
 */
public enum Permission {
    // Receipts (the walk-in billing screen)
    CREATE_BILL,
    PRINT_RECEIPT,
    DELETE_BILL,

    // Admin
    MANAGE_USERS,
    VIEW_AUDIT_LOG
}
