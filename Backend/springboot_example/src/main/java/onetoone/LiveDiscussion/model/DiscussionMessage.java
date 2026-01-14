package onetoone.LiveDiscussion.model;

import jakarta.persistence.*;
import onetoone.Users.model.Users;
import java.time.LocalDateTime;

@Entity
@Table(name = "discussion_messages")
public class DiscussionMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discussion_id")
    private Discussion discussion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_email", referencedColumnName = "emailId")
    private Users sender;

    @Column(columnDefinition = "TEXT", nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    // For direct messages
    private String recipientEmail;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean edited = false;

    private LocalDateTime editedAt;

    // Constructors
    public DiscussionMessage() {
        this.timestamp = LocalDateTime.now();
    }

    public DiscussionMessage(Discussion discussion, Users sender, String content, MessageType messageType) {
        this();
        this.discussion = discussion;
        this.sender = sender;
        this.content = content;
        this.messageType = messageType;
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

    public Users getSender() {
        return sender;
    }

    public void setSender(Users sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public boolean isEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }
}

