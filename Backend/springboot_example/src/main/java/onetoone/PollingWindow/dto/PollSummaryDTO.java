package onetoone.PollingWindow.dto;

import onetoone.Polling.model.Poll;

public class PollSummaryDTO {
    private Long id;
    private String title;
    private String type;
    private boolean active;

    public static PollSummaryDTO fromEntity(Poll poll) {
        if (poll == null) return null;
        PollSummaryDTO dto = new PollSummaryDTO();
        dto.id = poll.getId();
        dto.title = poll.getTitle();
        dto.type = poll.getType() != null ? poll.getType().toString() : null;
        dto.active = poll.isActive();
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

