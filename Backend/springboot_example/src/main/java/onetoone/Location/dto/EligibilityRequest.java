package onetoone.Location.dto;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class EligibilityRequest {
    private boolean eligible;
    private double distanceMeters;
    private String rule;
    private String serverTime;

    public EligibilityRequest(boolean eligible, double distanceMeters, String rule) {
        this.eligible = eligible;
        this.distanceMeters = distanceMeters;
        this.rule = rule;
        this.serverTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT); // Zulu time (UTC) for cleaner timestamps
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public void setDistanceMeters(double distanceMeters) {
        this.distanceMeters = distanceMeters;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }
}
