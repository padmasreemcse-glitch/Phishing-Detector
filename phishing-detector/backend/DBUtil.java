import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens MySQL connections for the classes that read scan history.
 *
 * Note: ScanDao keeps its own copy of these settings, so if you change the
 * password here, change it in ScanDao.java as well.
 */
public final class DBUtil {

    private static final String URL =
            "jdbc:mysql://localhost:3306/phishingdb";

    private static final String USER = "root";

    // Replace this with the database password
    private static final String PASSWORD = "";

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
