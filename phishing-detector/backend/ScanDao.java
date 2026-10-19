import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ScanDao {

    private static final String URL =
            "jdbc:mysql://localhost:3306/phishingdb";

    private static final String USER = "root";

    // Replace this with the database password
    private static final String PASSWORD = "";

    public void saveScan(String url, int score, String status)
            throws SQLException {

        String sql = "INSERT INTO scans "
                + "(url, risk_score, status) VALUES (?, ?, ?)";

        try (Connection con = DriverManager.getConnection(
                    URL, USER, PASSWORD);
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, url);
            ps.setInt(2, score);
            ps.setString(3, status);

            ps.executeUpdate();
        }
    }
}
