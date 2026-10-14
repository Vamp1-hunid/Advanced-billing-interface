package lodgebilling.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A booked or completed stay. The nightly rate and VAT % are copied in when the
 * booking is made, so later price changes do not alter this guest's bill.
 */
public record Reservation(long id, String guestName, String guestPhone, String company,
                          boolean companyPaysRoom, Room room, LocalDate checkIn, LocalDate checkOut,
                          ReservationStatus status, BigDecimal nightlyRate, BigDecimal vatPercent,
                          String notes, String createdBy) {

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean hasCompany() {
        return company != null && !company.isBlank();
    }

    /** Covers the night starting on this date (a guest leaving on day X does not occupy night X). */
    public boolean coversNight(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public String reference() {
        return String.format("BK%04d", id);   // BK = booking; receipts use R001
    }

    // JavaBean getters for TableView columns
    public String getReference()           { return reference(); }
    public String getGuestName()           { return guestName; }
    public String getRoomNumber()          { return room.number(); }
    public LocalDate getCheckIn()          { return checkIn; }
    public LocalDate getCheckOut()         { return checkOut; }
    public int getNights()                 { return nights(); }
    public ReservationStatus getStatus()   { return status; }
}
