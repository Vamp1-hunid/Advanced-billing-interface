package lodgebilling;

/**
 * Run THIS class from IntelliJ (green arrow), not LodgeBillingApp.
 *
 * Why: when the main class itself extends javafx.application.Application and
 * JavaFX sits on the classpath (not the module path), the JVM refuses to start
 * with "JavaFX runtime components are missing". A plain class that does not
 * extend Application skips that check, then hands over to the real app.
 */
public final class Launcher {

    private Launcher() { }

    public static void main(String[] args) {
        LodgeBillingApp.main(args);
    }
}
