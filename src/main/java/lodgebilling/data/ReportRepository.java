package lodgebilling.data;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Report queries, done in SQL so the database does the counting.
 *
 * SQLite has no decimal type and money is stored as text, so amounts are turned
 * into whole ngwee (integer cents) before SUM. Adding integers is exact; adding
 * floating-point numbers is not.
 */
public class ReportRepository {

    /** One day of the daily report. Revenue figures are before discount and VAT. */
    public record DailyStat(LocalDate date, int rooms, int roomsSold, BigDecimal roomRevenue,
                            BigDecimal paymentsReceived) {

        public double occupancyPercent() {
            return rooms == 0 ? 0 : 100.0 * roomsSold / rooms;
        }

        /** Average daily rate: room revenue per room actually sold. */
        public BigDecimal adr() {
            return roomsSold == 0 ? BigDecimal.ZERO
                    : roomRevenue.divide(BigDecimal.valueOf(roomsSold), 2, java.math.RoundingMode.HALF_UP);
        }

        /** Revenue per available room: room revenue spread over every room, sold or not. */
        public BigDecimal revPar() {
            return rooms == 0 ? BigDecimal.ZERO
                    : roomRevenue.divide(BigDecimal.valueOf(rooms), 2, java.math.RoundingMode.HALF_UP);
        }
    }

    // A recursive CTE generates one row per day in the range, so days with no
    // bookings still appear (with zeros) instead of silently missing.
    private static final String DAILY = """
            WITH RECURSIVE days(d) AS (
                SELECT ?
                UNION ALL
                SELECT date(d, '+1 day') FROM days WHERE d < ?
            ),
            stays AS (
                SELECT check_in, check_out,
                       CAST(ROUND(CAST(nightly_rate AS REAL) * 100) AS INTEGER) AS rate_ngwee
                FROM reservations
                WHERE status IN ('CHECKED_IN', 'CHECKED_OUT')
            ),
            pay AS (
                SELECT substr(posted_at, 1, 10) AS d,
                       SUM(CAST(ROUND(CAST(amount AS REAL) * 100) AS INTEGER)) AS ngwee
                FROM folio_entries WHERE kind = 'PAYMENT' AND voided = 0
                GROUP BY 1
                UNION ALL
                SELECT substr(issued_at, 1, 10),
                       SUM(CAST(ROUND(CAST(paid AS REAL) * 100) AS INTEGER))
                FROM bills GROUP BY 1
            )
            SELECT days.d,
                   (SELECT COUNT(*) FROM rooms)                                          AS rooms,
                   (SELECT COUNT(*) FROM stays WHERE check_in <= days.d AND check_out > days.d) AS sold,
                   (SELECT COALESCE(SUM(rate_ngwee), 0) FROM stays
                     WHERE check_in <= days.d AND check_out > days.d)                    AS revenue_ngwee,
                   (SELECT COALESCE(SUM(ngwee), 0) FROM pay WHERE pay.d = days.d)        AS paid_ngwee
            FROM days
            ORDER BY days.d
            """;

    private final Connection db;

    public ReportRepository(Database database) {
        this.db = database.connection();
    }

    public List<DailyStat> daily(LocalDate from, LocalDate to) {
        List<DailyStat> rows = new ArrayList<>();
        try (PreparedStatement ps = db.prepareStatement(DAILY)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new DailyStat(LocalDate.parse(rs.getString(1)), rs.getInt(2), rs.getInt(3),
                            BigDecimal.valueOf(rs.getLong(4), 2), BigDecimal.valueOf(rs.getLong(5), 2)));
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DataException("Could not build the daily report", e);
        }
    }
}
