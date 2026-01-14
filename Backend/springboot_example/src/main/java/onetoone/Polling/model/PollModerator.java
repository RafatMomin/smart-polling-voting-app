package onetoone.Polling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import onetoone.Users.model.Users;
import java.time.LocalDateTime;

@Entity
@Table(name = "poll_moderator")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PollModerator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id", nullable = false)
    @JsonIgnore
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_email", referencedColumnName = "emailId", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Users moderator;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    @Column(nullable = true)
    private LocalDateTime expiresAt; // null means "forever"

    @Column(nullable = false)
    private boolean isActive = true; // can be manually revoked

    public PollModerator() {
        this.assignedAt = LocalDateTime.now();
    }

    public PollModerator(Poll poll, Users moderator) {
        this();
        this.poll = poll;
        this.moderator = moderator;
    }

    public PollModerator(Poll poll, Users moderator, LocalDateTime expiresAt) {
        this(poll, moderator);
        this.expiresAt = expiresAt;
    }

    /**
     * Check if this moderator assignment is currently valid
     */
    public boolean isCurrentlyValid() {
        if (!isActive) {
            return false;
        }

        // If expiresAt is null, it means "forever"
        if (expiresAt == null) {
            return true;
        }

        return LocalDateTime.now().isBefore(expiresAt);
    }

    /**
     * Check if this is a "forever" access assignment
     */
    public boolean isForeverAccess() {
        return expiresAt == null;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public Users getModerator() {
        return moderator;
    }

    public void setModerator(Users moderator) {
        this.moderator = moderator;
    }

    public String getModeratorEmail() {
        return moderator != null ? moderator.getEmailId() : null;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
