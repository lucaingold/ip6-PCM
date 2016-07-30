package ch.fhnw.ip6.powerconsumptionmanager.model;

/**
 * Holds the loaded route information to an origin and destination
 */
public class RouteInformation {
    private String mDurationText;
    private String mDistanceText;

    public RouteInformation(String durationText, String distanceText) {
        this.mDurationText = durationText;
        this.mDistanceText = distanceText;
    }

    public String getDurationText() {
        return mDurationText;
    }

    public String getDistanceText() {
        return mDistanceText;
    }
}