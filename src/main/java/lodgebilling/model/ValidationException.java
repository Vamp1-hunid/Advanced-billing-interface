package lodgebilling.model;

/** Thrown when the form is invalid. The message is shown to the user as-is. */
public class ValidationException extends Exception {

    private final FormField field;

    public ValidationException(String message, FormField field) {
        super(message);
        this.field = field;
    }

    /** The input that should get focus so the user can fix it. */
    public FormField field() {
        return field;
    }
}
