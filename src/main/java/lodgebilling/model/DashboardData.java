package lodgebilling.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Everything the front-desk dashboard shows, worked out in one go. */
public record DashboardData(List<Reservation> arrivals, List<Reservation> departures, int inHouse,
                            int totalRooms, Map<RoomStatus, Integer> roomsByStatus,
                            BigDecimal outstanding, BigDecimal paymentsToday) {

    public double occupancyPercent() {
        return totalRooms == 0 ? 0 : 100.0 * inHouse / totalRooms;
    }
}
