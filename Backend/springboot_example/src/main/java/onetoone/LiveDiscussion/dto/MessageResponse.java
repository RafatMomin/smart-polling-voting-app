package onetoone.LiveDiscussion.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class MessageResponse {
    private Long id;
    private Map<String, String> user;
    private String content;
    private String messageType;
    private String recipientEmail;
    private LocalDateTime timestamp;
    private boolean edited;

    public MessageResponse(Long id, Map<String, String> user, String content,
                           String messageType, String recipientEmail,
                           LocalDateTime timestamp, boolean edited) {
        this.id = id;
        this.user = user;
        this.content = content;
        this.messageType = messageType;
        this.recipientEmail = recipientEmail;
        this.timestamp = timestamp;
        this.edited = edited;
    }

    // Getters
    public Long getId() { return id; }
    public Map<String, String> getUser() { return user; }
    public String getContent() { return content; }
    public String getMessageType() { return messageType; }
    public String getRecipientEmail() { return recipientEmail; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isEdited() { return edited; }
}