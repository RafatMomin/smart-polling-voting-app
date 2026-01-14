package onetoone.LiveDiscussion.model;

import jakarta.persistence.*;
import onetoone.Polling.model.Poll;
import onetoone.Users.model.Users;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discussions")
public class Discussion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscussionType type;

    // Optional: Link to a specific poll (for POLL_DISCUSSION type)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    private Poll poll;

    // Host user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_user_email", referencedColumnName = "emailId")
    private Users hostUser;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime closedAt;

    @Column(nullable = false)
    private boolean active = true;

    // Tags for general discussions (stored as JSON or comma-separated)
    @ElementCollection
    @CollectionTable(name = "discussion_tags", joinColumns = @JoinColumn(name = "discussion_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column
    private LocalDateTime lastActivity;

    // Constructors
    public Discussion() {
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    public Discussion(String title, String description, DiscussionType type, Users hostUser) {
        this();
        this.title = title;
        this.description = description;
        this.type = type;
        this.hostUser = hostUser;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscussionType getType() {
        return type;
    }

    public void setType(DiscussionType type) {
        this.type = type;
    }

    public Poll getPoll() {
        return poll;
    }

    public void setPoll(Poll poll) {
        this.poll = poll;
    }

    public Users getHostUser() {
        return hostUser;
    }

    public void setHostUser(Users hostUser) {
        this.hostUser = hostUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
}

