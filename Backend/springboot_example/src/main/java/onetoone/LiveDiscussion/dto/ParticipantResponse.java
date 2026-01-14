package onetoone.LiveDiscussion.dto;

import java.time.LocalDateTime;

public class ParticipantResponse {
    private String emailId;
    private String name;
    private LocalDateTime joinedAt;
    private boolean isHost;
    private boolean isOnline;
    private LocalDateTime lastSeen;

    public ParticipantResponse(String emailId, String name, LocalDateTime joinedAt,
                               boolean isHost, boolean isOnline, LocalDateTime lastSeen) {
        this.emailId = emailId;
        this.name = name;
        this.joinedAt = joinedAt;
        this.isHost = isHost;
        this.isOnline = isOnline;
        this.lastSeen = lastSeen;
    }

    // Getters
    public String getEmailId() { return emailId; }
    public String getName() { return name; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public boolean isHost() { return isHost; }
    public boolean isOnline() { return isOnline; }
    public LocalDateTime getLastSeen() { return lastSeen; }
}