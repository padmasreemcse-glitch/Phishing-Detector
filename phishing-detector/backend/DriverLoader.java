import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class DriverLoader implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            event.getServletContext().log("MySQL JDBC driver loaded");
        } catch (ClassNotFoundException e) {
            event.getServletContext().log(
                    "MySQL JDBC driver not found in WEB-INF/lib", e);
        }
    }
}
