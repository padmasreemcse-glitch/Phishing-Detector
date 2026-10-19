import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** GET /history - returns the most recent scans as a JSON array. */
@WebServlet("/history")
public class HistoryServlet extends HttpServlet {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        int limit = parseLimit(request.getParameter("limit"));

        String sql = "SELECT id, url, risk_score, status, scanned_at "
                + "FROM scans ORDER BY scanned_at DESC, id DESC LIMIT ?";

        List<ScanResult> rows = new ArrayList<>();

        try (Connection con = DBUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ScanResult(
                            rs.getInt("id"),
                            rs.getString("url"),
                            rs.getInt("risk_score"),
                            rs.getString("status"),
                            rs.getTimestamp("scanned_at")));
                }
            }

        } catch (SQLException e) {
            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(
                    "{\"error\":\"Database operation failed\"}");
            log("Unable to load scan history", e);
            return;
        }

        PrintWriter out = response.getWriter();
        out.write("[");
        for (int i = 0; i < rows.size(); i++) {
            ScanResult r = rows.get(i);
            if (i > 0) {
                out.write(",");
            }
            out.write("{\"id\":" + r.getId()
                    + ",\"url\":\"" + escape(r.getUrl())
                    + "\",\"riskScore\":" + r.getRiskScore()
                    + ",\"status\":\"" + escape(r.getStatus())
                    + "\",\"scannedAt\":\""
                    + (r.getScannedAt() == null
                            ? "" : r.getScannedAt().toString())
                    + "\"}");
        }
        out.write("]");
    }

    private int parseLimit(String raw) {
        if (raw == null) {
            return DEFAULT_LIMIT;
        }
        try {
            int n = Integer.parseInt(raw.trim());
            return Math.max(1, Math.min(n, MAX_LIMIT));
        } catch (NumberFormatException e) {
            return DEFAULT_LIMIT;
        }
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
