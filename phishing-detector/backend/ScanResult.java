import java.sql.Timestamp;

/** One saved row of the scans table. */
public class ScanResult {

    private final int id;
    private final String url;
    private final int riskScore;
    private final String status;
    private final Timestamp scannedAt;

    public ScanResult(int id, String url, int riskScore,
                      String status, Timestamp scannedAt) {
        this.id = id;
        this.url = url;
        this.riskScore = riskScore;
        this.status = status;
        this.scannedAt = scannedAt;
    }

    public int getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public String getStatus() {
        return status;
    }

    public Timestamp getScannedAt() {
        return scannedAt;
    }
}
