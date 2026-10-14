package lodgebilling.security;

/**
 * Things a user may be allowed to do. Code checks permissions, never roles,
 * so adding a role only means editing {@link Role}.
 */
public enum Permission {
    // Receipts (the original walk-in billing screen)
    CREATE_BILL,
    PRINT_RECEIPT,
    DELETE_BILL,

    // Front desk
    MANAGE_RESERVATIONS,   // book, edit, cancel, check in, check out

    // Rooms
    UPDATE_ROOM_STATUS,    // clean / dirty / maintenance
    MANAGE_ROOMS,          // add rooms, change rates and VAT

    // Admin
    MANAGE_USERS,
    VIEW_AUDIT_LOG
}
