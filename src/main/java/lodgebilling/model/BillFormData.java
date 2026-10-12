package lodgebilling.model;


/** The raw, unvalidated values as typed into the form. */
public record BillFormData(String name, RoomType room, String nights,
                           String extras, String paid, String notes) { }
