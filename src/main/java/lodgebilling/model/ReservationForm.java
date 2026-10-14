package lodgebilling.model;

import java.time.LocalDate;

/** What the booking dialog collects, before LodgeService checks it. */
public record ReservationForm(String guestName, String phone, String company, boolean companyPaysRoom,
                              Room room, LocalDate checkIn, LocalDate checkOut, String notes) {

    public boolean hasCompany() {
        return company != null && !company.isBlank();
    }
}
