package onetoone.PollingWindow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import onetoone.Polling.model.Poll;
import onetoone.Users.model.Users;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a time-based voting window for a poll
 *
 * HYBRID LOGIC:
 * - Window must be manually activated (isActive = true)
 * - Once active, time boundaries are enforced
 * - Manual close overrides time window
 * - Can reopen if still within time boundaries
 */
@Entity
@Table(name = "polling_windows")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PollingWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnore
    private Poll poll;

    private String name;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    // Manual activation flag
    private boolean isActive = false;

    // Track if window was manually closed (overrides time window)
    private boolean manuallyClosed = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_email", referencedColumnName = "emailId", nullable = false)
    @JsonIgnore
    private Users creator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "window_groups",
            joinColumns = @JoinColumn(name = "window_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @JsonIgnore
    private Set<Group> targetGroups = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "window_individual_users",
            joinColumns = @JoinColumn(name = "window_id"),
            inverseJoinColumns = @JoinColumn(name = "user_email", referencedColumnName = "emailId")
    )
    @JsonIgnore
    private Set<Users> targetIndividualUsers = new HashSet<>();

    public PollingWindow() {
        this.startTime = LocalDateTime.now();
    }

    public PollingWindow(Poll poll, String name, Users creator) {
        this.poll = poll;
        this.name = name;
        this.creator = creator;
        this.startTime = LocalDateTime.now();
    }

    /**
     * HYBRID LOGIC: Check if window is currently open for voting
     *
     * Rules:
     * 1. Must be manually activated (isActive = true)
     * 2. Must be within time boundaries (startTime <= now < endTime)
     * 3. If manually closed, stays closed until manually reopened
     * 4. If reopened, time boundaries are still enforced
     */
    public boolean isCurrentlyOpen() {
        // Rule 1: Must be manually activated
        if (!isActive) {
            return false;
        }

        // Rule 2: Check time boundaries
        if (!isWithinTimeWindow()) {
            return false;
        }

        // If all checks pass, window is open
        return true;
    }

    /**
     * Check if current time is within the window's time boundaries
     */
    public boolean isWithinTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = startTime == null || now.isAfter(startTime) || now.isEqual(startTime);
        boolean beforeEnd = endTime == null || now.isBefore(endTime);
        return afterStart && beforeEnd;
    }

    /**
     * Get the status of the window with detailed reason
     */
    public String getWindowStatus() {
        if (!isActive && !manuallyClosed) {
            return "NOT_OPENED"; // Never been opened
        }

        if (manuallyClosed) {
            if (isWithinTimeWindow()) {
                return "MANUALLY_CLOSED"; // Closed early, but could reopen
            } else {
                return "EXPIRED_AND_CLOSED"; // Past end time and closed
            }
        }

        if (isActive) {
            if (!isWithinTimeWindow()) {
                LocalDateTime now = LocalDateTime.now();
                if (startTime != null && now.isBefore(startTime)) {
                    return "ACTIVE_BUT_NOT_STARTED"; // Opened but before start time
                } else {
                    return "ACTIVE_BUT_EXPIRED"; // Opened but past end time
                }
            }
            return "OPEN"; // Active and within time window
        }

        return "UNKNOWN";
    }

    /**
     * Open the window (manual activation)
     * Returns true if successful, false if cannot open
     */
    public boolean open() {
        // Can only open if within time boundaries
        if (!isWithinTimeWindow()) {
            return false;
        }

        this.isActive = true;
        this.manuallyClosed = false;
        return true;
    }

    /**
     * Close the window (manual deactivation)
     */
    public void close() {
        this.isActive = false;
        this.manuallyClosed = true;
    }

    /**
     * Force open (bypass time restrictions - use for admin override)
     */
    public void forceOpen() {
        this.isActive = true;
        this.manuallyClosed = false;
    }

    public boolean canUserVote(String userEmail) {
        if (!isCurrentlyOpen()) return false;

        if (targetGroups.isEmpty() && targetIndividualUsers.isEmpty()) {
            return true;
        }

        for (Users user : targetIndividualUsers) {
            if (user.getEmailId().equals(userEmail)) {
                return true;
            }
        }

        for (Group group : targetGroups) {
            if (group.isMember(userEmail)) {
                return true;
            }
        }

        return false;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Poll getPoll() { return poll; }
    public void setPoll(Poll poll) { this.poll = poll; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isManuallyClosed() { return manuallyClosed; }
    public void setManuallyClosed(boolean manuallyClosed) { this.manuallyClosed = manuallyClosed; }

    public Users getCreator() { return creator; }
    public void setCreator(Users creator) { this.creator = creator; }

    public String getCreatorEmail() {
        return creator != null ? creator.getEmailId() : null;
    }

    public Set<Group> getTargetGroups() { return targetGroups; }
    public void setTargetGroups(Set<Group> targetGroups) { this.targetGroups = targetGroups; }

    public Set<Users> getTargetIndividualUsers() { return targetIndividualUsers; }
    public void setTargetIndividualUsers(Set<Users> targetIndividualUsers) {
        this.targetIndividualUsers = targetIndividualUsers;
    }

    public Set<String> getTargetIndividualUserEmails() {
        Set<String> emails = new HashSet<>();
        for (Users user : targetIndividualUsers) {
            emails.add(user.getEmailId());
        }
        return emails;
    }

    public void addTargetGroup(Group group) {
        this.targetGroups.add(group);
    }

    public void addTargetUser(Users user) {
        this.targetIndividualUsers.add(user);
    }

    public void removeTargetUser(Users user) {
        this.targetIndividualUsers.remove(user);
    }

    public void removeTargetGroup(Group group) {
        this.targetGroups.remove(group);
    }
}