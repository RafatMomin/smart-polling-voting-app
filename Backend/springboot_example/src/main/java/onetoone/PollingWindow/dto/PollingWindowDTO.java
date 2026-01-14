package onetoone.PollingWindow.dto;

import onetoone.PollingWindow.model.PollingWindow;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO for PollingWindow responses - matches API documentation format
 */
public class PollingWindowDTO {
    private Long id;
    private PollSummaryDTO poll;
    private String name;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean active;
    private String creatorEmail;
    private Set<GroupDTO> targetGroups;
    private Set<String> targetIndividualUserEmails;

    public static PollingWindowDTO fromEntity(PollingWindow window) {
        PollingWindowDTO dto = new PollingWindowDTO();
        dto.id = window.getId();
        dto.poll = window.getPoll() != null ? PollSummaryDTO.fromEntity(window.getPoll()) : null;
        dto.name = window.getName();
        dto.startTime = window.getStartTime();
        dto.endTime = window.getEndTime();
        dto.active = window.isActive();
        dto.creatorEmail = window.getCreatorEmail();

        // Convert target groups to DTOs (handle nulls defensively)
        dto.targetGroups = window.getTargetGroups() != null
                ? window.getTargetGroups().stream()
                    .map(GroupDTO::fromEntity)
                    .collect(Collectors.toSet())
                : null;

        // Get individual user emails
        dto.targetIndividualUserEmails = window.getTargetIndividualUserEmails();

        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PollSummaryDTO getPoll() { return poll; }
    public void setPoll(PollSummaryDTO poll) { this.poll = poll; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getCreatorEmail() { return creatorEmail; }
    public void setCreatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; }

    public Set<GroupDTO> getTargetGroups() { return targetGroups; }
    public void setTargetGroups(Set<GroupDTO> targetGroups) { this.targetGroups = targetGroups; }

    public Set<String> getTargetIndividualUserEmails() { return targetIndividualUserEmails; }
    public void setTargetIndividualUserEmails(Set<String> targetIndividualUserEmails) {
        this.targetIndividualUserEmails = targetIndividualUserEmails;
    }
}
