package onetoone.Polling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import onetoone.PollingWindow.model.PollingWindow;
import java.util.ArrayList;
import onetoone.Users.model.Users;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "polls")
public abstract class Poll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private boolean isActive = true;

    private String type;

    private String creatorEmail; // Who created this poll

    // NEW: Context field for RAG
    @Column(columnDefinition = "TEXT")
    private String context;

    // RAG embedding
    @Column(columnDefinition = "TEXT")
    private String embedding;

    // NEW: One-to-Many relationship with PollingWindows
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollingWindow> pollingWindows = new ArrayList<>();

    // ONE-TO-MANY relationship with PollModerators
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PollModerator> moderators = new ArrayList<>();

    // Text that will be embedded
    public String getTextRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "Poll ID: %s, Title: \"%s\", Type: %s, Active: %s",
                id != null ? id : "new",
                title,
                getType(),
                isActive
        ));

        if (context != null && !context.trim().isEmpty()) {
            sb.append(", Context: ").append(context);
        }

        return sb.toString();
    }
    // private/public visibility
    public enum Visibility {
        PUBLIC, PRIVATE
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility = Visibility.PUBLIC;

    // Admin/Owner (One-to-One relationship)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_email", referencedColumnName = "emailId")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Users admin; // creator of poll

    // access code
    @Column(nullable = true, unique = true, length = 10)
    private String accessCode;

    // Members (Many-to-Many relationship via PollMember)
    @OneToMany(mappedBy = "poll", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PollMember> members = new ArrayList<>();

    // timestamp
    private LocalDateTime createdAt;

    // votes relationship is optional to map here; we will rely on VoteRepository
    // to fetch stats; mapping votes here may cause large fetches -> omit to avoid N+1/large loads

    public Poll() {}
    public Poll(String title) { this.title = title; }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getCreatorEmail() { return creatorEmail; }
    public void setCreatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; }
    public String getContext() { return context; }
    public void setContext(String context) { this.context = context; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public List<PollingWindow> getPollingWindows() { return pollingWindows; }
    public void setPollingWindows(List<PollingWindow> pollingWindows) {
        this.pollingWindows = pollingWindows;
    }

    public void addPollingWindow(PollingWindow window) {
        pollingWindows.add(window);
        window.setPoll(this);
    }

    public abstract void validateVote(Vote vote);
    public abstract String getType();

    // private/public getters/setters
    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Users getOwner() {
        return admin;
    }

    public void setOwner(Users owner) {
        this.admin = owner;
    }

    public String getAccessCode() {
        return accessCode;
    }

    public void setAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public List<PollMember> getMembers() {
        return members;
    }

    public void setMembers(List<PollMember> members) {
        this.members = members;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PollModerator> getModerators() {
        return moderators;
    }

    public void setModerators(List<PollModerator> moderators) {
        this.moderators = moderators;
    }
}