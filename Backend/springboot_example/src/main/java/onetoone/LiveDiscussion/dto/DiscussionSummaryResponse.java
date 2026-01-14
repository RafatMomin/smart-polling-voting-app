package onetoone.LiveDiscussion.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DiscussionSummaryResponse {
    private Long id;
    private String title;
    private String type;
    private Long pollId;
    private Map<String, String> hostUser;
    private LocalDateTime createdAt;
    private boolean active;
    private long participantCount;
    private long messageCount;
    private LocalDateTime lastActivity;
    private List<String> tags;

    public DiscussionSummaryResponse(Long id, String title, String type, Long pollId,
                                     Map<String, String> hostUser, LocalDateTime createdAt,
                                     boolean active, long participantCount, long messageCount,
                                     LocalDateTime lastActivity, List<String> tags) {
        this.id = id;
        this.title = title;
        this.type = type;
        this.pollId = pollId;
        this.hostUser = hostUser;
        this.createdAt = createdAt;
        this.active = active;
        this.participantCount = participantCount;
        this.messageCount = messageCount;
        this.lastActivity = lastActivity;
        this.tags = tags;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getType() { return type; }
    public Long getPollId() { return pollId; }
    public Map<String, String> getHostUser() { return hostUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public long getParticipantCount() { return participantCount; }
    public long getMessageCount() { return messageCount; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public List<String> getTags() { return tags; }
}