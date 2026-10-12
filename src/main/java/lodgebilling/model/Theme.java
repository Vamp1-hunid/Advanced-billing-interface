package lodgebilling.model;

/** The two colour schemes the user can switch between. */
public enum Theme {
    LIGHT, DARK;

    public Theme toggled() {
        return this == LIGHT ? DARK : LIGHT;
    }
}
