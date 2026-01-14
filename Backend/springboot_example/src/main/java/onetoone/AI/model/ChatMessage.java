package onetoone.AI.model;

import jakarta.persistence.*;
import onetoone.Polling.model.Poll;
import onetoone.Users.model.Users;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many-to-One: Many ChatMessages belong to One User
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_email", referencedColumnName = "emailId", nullable = false)
    private Users user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String userMessage;

    @Column(columnDefinition = "TEXT")
    private String botResponse;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Many-to-Many: ChatMessage can reference multiple Polls
    @ManyToMany
    @JoinTable(
            name = "chat_poll_references",
            joinColumns = @JoinColumn(name = "chat_message_id"),
            inverseJoinColumns = @JoinColumn(name = "poll_id")
    )
    private List<Poll> referencedPolls = new ArrayList<>();

    private Long responseTimeMs;

    // Constructors
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public ChatMessage(Users user, String userMessage) {
        this();
        this.user = user;
        this.userMessage = userMessage;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getBotResponse() {
        return botResponse;
    }

    public void setBotResponse(String botResponse) {
        this.botResponse = botResponse;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<Poll> getReferencedPolls() {
        return referencedPolls;
    }

    public void setReferencedPolls(List<Poll> referencedPolls) {
        this.referencedPolls = referencedPolls;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }
}