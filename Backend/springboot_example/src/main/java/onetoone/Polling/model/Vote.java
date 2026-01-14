package onetoone.Polling.model;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votes")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user identifier (could be email or userId)
    private String userId;

    // text choice for multiple-choice and ranking
    private String choice;

    // numeric value for rating (1-5) and yes/no (0/1)
    private Integer numericValue;

    // rank for ranking polls (1..N)
    private Integer rank;

    // geofence user location tracking, if location not provided the value is null
    private Double latitude;
    private Double longitude;
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Poll poll;

    public Vote() {
        this.timestamp = LocalDateTime.now();
    }

    // convenience ctors
    public Vote(String userId, Integer numericValue) {
        this.userId = userId;
        this.numericValue = numericValue;
        this.timestamp = LocalDateTime.now();
    }

    public Vote(String userId, String choice) {
        this.userId = userId;
        this.choice = choice;
        this.timestamp = LocalDateTime.now();
    }

    public Vote(String userId, String choice, Integer rank) {
        this.userId = userId;
        this.choice = choice;
        this.rank = rank;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getChoice() { return choice; }
    public void setChoice(String choice) { this.choice = choice; }
    public Integer getNumericValue() { return numericValue; }
    public void setNumericValue(Integer numericValue) { this.numericValue = numericValue; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    // getters and setters for location validation
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public void setLatitude(double lat) {
        this.latitude = lat;
    }
    public void setLongitude(double lng) {
        this.longitude = lng;
    }
}