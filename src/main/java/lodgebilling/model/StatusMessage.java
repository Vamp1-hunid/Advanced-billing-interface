package lodgebilling.model;

/**
 * A message for the user, plus how it should be presented (neutral, good news, problem).
 * An error about one input names that field so the form can highlight it; otherwise field is null.
 */
public record StatusMessage(String text, Kind kind, FormField field) {

    public enum Kind { INFO, SUCCESS, ERROR }

    public static StatusMessage info(String text)    { return new StatusMessage(text, Kind.INFO, null); }
    public static StatusMessage success(String text) { return new StatusMessage(text, Kind.SUCCESS, null); }
    public static StatusMessage error(String text)   { return new StatusMessage(text, Kind.ERROR, null); }

    /** An error caused by one input, e.g. letters typed into Nights. */
    public static StatusMessage error(String text, FormField field) {
        return new StatusMessage(text, Kind.ERROR, field);
    }

    public boolean isError() {
        return kind == Kind.ERROR;
    }
}
