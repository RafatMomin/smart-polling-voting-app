package onetoone.LiveDiscussion.dto;

import onetoone.Polling.model.Poll;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class DiscussionDetailResponse {
    private Long id;
    private String title;
    private String description;
    private String type;
    private Long pollId;
    private Poll poll;
    private Map<String, String> hostUser;
    private List<ParticipantResponse> participants;
    private LocalDateTime createdAt;
    private boolean active;
    private long participantCount;
    private long messageCount;
    private LocalDateTime lastActivity;
    private String websocketEndpoint;

    public DiscussionDetailResponse(Long id, String title, String description, String type,
                                    Long pollId, Poll poll, Map<String, String> hostUser,
                                    List<ParticipantResponse> participants, LocalDateTime createdAt,
                                    boolean active, long participantCount, long messageCount,
                                    LocalDateTime lastActivity, String websocketEndpoint) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.pollId = pollId;
        this.poll = poll;
        this.hostUser = hostUser;
        this.participants = participants;
        this.createdAt = createdAt;
        this.active = active;
        this.participantCount = participantCount;
        this.messageCount = messageCount;
        this.lastActivity = lastActivity;
        this.websocketEndpoint = websocketEndpoint;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public Long getPollId() { return pollId; }
    public Poll getPoll() { return poll; }
    public Map<String, String> getHostUser() { return hostUser; }
    public List<ParticipantResponse> getParticipants() { return participants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isActive() { return active; }
    public long getParticipantCount() { return participantCount; }
    public long getMessageCount() { return messageCount; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public String getWebsocketEndpoint() { return websocketEndpoint; }
}