import java.net.URI;

public class UrlAnalyzer {

    public static class Analysis {
        public int riskScore;
        public String status;

        public Analysis(int riskScore, String status) {
            this.riskScore = riskScore;
            this.status = status;
        }
    }

    public Analysis analyze(String url) {
        if (url == null || url.trim().isEmpty()) {
            return new Analysis(0, "Invalid URL");
        }

        url = url.trim();
        int score = 0;

        try {
            URI uri = new URI(url);
            String host = uri.getHost();

            if (host == null || uri.getScheme() == null) {
                return new Analysis(0, "Invalid URL");
            }

            String scheme = uri.getScheme().toLowerCase();

            if (!scheme.equals("http") && !scheme.equals("https")) {
                return new Analysis(0, "Invalid URL");
            }

            if (!scheme.equals("https")) {
                score += 20;
            }

            if (url.contains("@")) {
                score += 20;
            }

            String lowerUrl = url.toLowerCase();

            if (lowerUrl.contains("login")
                    || lowerUrl.contains("verify")
                    || lowerUrl.contains("password")) {
                score += 20;
            }

            if (url.length() > 100) {
                score += 20;
            }

            if (host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
                score += 20;
            }

            String status = score >= 40
                    ? "Suspicious"
                    : "No major warning detected";

            return new Analysis(score, status);

        } catch (Exception e) {
            return new Analysis(0, "Invalid URL");
        }
    }
}
