package onetoone.Location.service;


import onetoone.Location.dto.EligibilityRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    @Autowired
    private CampusConfig campusConfig;

    /* calculate distance between 2 coordinates using Haversine distance formula
     * returns distance in meters */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_METERS = 6371000; // Earth's radius in meters

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    // check if a location is in the campus geofence
    public EligibilityRequest checkEligibility(double lat, double lng) {
        double centerLat = campusConfig.getCenter().getLatitude();
        double centerLng = campusConfig.getCenter().getLongitude();
        double radiusMeters = campusConfig.getRadius().getMeters();

        double distance = calculateDistance(lat, lng, centerLat, centerLng);
        boolean eligible = distance <= radiusMeters;

        String rule = String.format("within %.0f m of ISU campus center", radiusMeters);

        return new EligibilityRequest(eligible, Math.round(distance * 100.0) / 100.0, rule);
    }

    // validate that that location is in the geofence, throw exception if its not
    public void validateLocation(double lat, double lng) {
        EligibilityRequest response = checkEligibility(lat, lng);
        if (!response.isEligible()) {
            throw new RuntimeException("User is outside the campus geofence");
        }
    }

}
