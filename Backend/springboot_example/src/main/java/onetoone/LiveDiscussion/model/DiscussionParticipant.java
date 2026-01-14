package onetoone.LiveDiscussion.model;

import jakarta.persistence.*;
import onetoone.Users.model.Users;
import java.time.LocalDateTime;

@Entity
@Table(name = "discussion_participants")
public class DiscussionParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discussion_id")
    private Discussion discussion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_email", referencedColumnName = "emailId")
    private Users user;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    @Column(nullable = false)
    private boolean isHost;

    private LocalDateTime lastSeen;

    @Column(nullable = false)
    private boolean isOnline = false;

    // Constructors
    public DiscussionParticipant() {
        this.joinedAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }

    public DiscussionParticipant(Discussion discussion, Users user, boolean isHost) {
        this();
        this.discussion = discussion;
        this.user = user;
        this.isHost = isHost;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Discussion getDiscussion() {
        return discussion;
    }

    public void setDiscussion(Discussion discussion) {
        this.discussion = discussion;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }

    public LocalDateTime getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}

