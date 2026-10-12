package lodgebilling.model;

/** The inputs on the bill form. Validation errors point at all of these except NOTES. */
public enum FormField {
    NAME, ROOM, NIGHTS, EXTRAS, PAID, NOTES
}
