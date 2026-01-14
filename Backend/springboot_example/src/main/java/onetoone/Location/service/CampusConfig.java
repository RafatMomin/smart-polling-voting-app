package onetoone.Location.service;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "campus")
public class CampusConfig {

    // CENTER class
    public static class Center {
        private double latitude;
        private double longitude;

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
    }

    // RADIUS class
    public static class Radius {
        private double meters;

        public double getMeters() {
            return meters;
        }

        public void setMeters(double meters) {
            this.meters = meters;
        }
    }

    private Center center = new Center();
    private Radius radius = new Radius();

    // getters and setters
    public Center getCenter() {
        return center;
    }
    public void setCenter(Center center) {
        this.center = center;
    }
    public Radius getRadius() {
        return radius;
    }
    public void setRadius(Radius radius) {
        this.radius = radius;
    }

}
