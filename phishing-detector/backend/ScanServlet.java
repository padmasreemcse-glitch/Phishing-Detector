import java.io.IOException;
import java.sql.SQLException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/scan")
public class ScanServlet extends HttpServlet {

    private final UrlAnalyzer analyzer = new UrlAnalyzer();
    private final ScanDao dao = new ScanDao();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String url = request.getParameter("url");

        if (url == null || url.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"error\":\"URL is required\"}");
            return;
        }

        UrlAnalyzer.Analysis result = analyzer.analyze(url);

        if ("Invalid URL".equals(result.status)) 
     {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(
                    "{\"error\":\"Invalid URL\"}");
            return;
        }

        try {
            dao.saveScan(url, result.riskScore, result.status);

            response.getWriter().write("{\"url\":\"" + escape(url)
                    + "\",\"riskScore\":" + result.riskScore
                    + ",\"status\":\"" + result.status + "\"}");

        } catch (SQLException e) {
            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getWriter().write(
                    "{\"error\":\"Database operation failed\"}");

            log("Unable to save scan result", e);
        }
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"");
    }
}
