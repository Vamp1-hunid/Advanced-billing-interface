package lodgebilling.view;

import lodgebilling.security.Permission;

/** The screens in the sidebar, in order, with who may see each one. */
public enum AppPage {
    QUICK_BILLS("Receipts", Permission.CREATE_BILL),
    ROOMS("Rooms", Permission.UPDATE_ROOM_STATUS);

    private final String label;
    private final Permission permission;

    AppPage(String label, Permission permission) {
        this.label = label;
        this.permission = permission;
    }

    public String label() {
        return label;
    }

    public Permission permission() {
        return permission;
    }
}
