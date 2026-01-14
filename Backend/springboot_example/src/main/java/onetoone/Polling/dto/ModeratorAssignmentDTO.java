package onetoone.Polling.dto;

import onetoone.Polling.model.PollModerator;
import java.time.LocalDateTime;

public class ModeratorAssignmentDTO {
    private String moderatorEmail;
    private String duration; // e.g., "2h", "3d", "1w", "forever"
    private Integer durationValue; // numeric value (optional, alternative to duration string)
    private String durationUnit; // "HOURS", "DAYS", "WEEKS", or "FOREVER"
    private LocalDateTime expiresAt; // calculated or provided directly

    public ModeratorAssignmentDTO() {}

    public ModeratorAssignmentDTO(String moderatorEmail, String durationUnit) {
        this.moderatorEmail = moderatorEmail;
        this.durationUnit = durationUnit;
    }

    // calculates expiration time based on given duration
    public LocalDateTime calculateExpiresAt () {
        if (duration == null || duration.trim().isEmpty()) {
            return null; // forever
        }

        String d = duration.trim().toLowerCase();
        if (d.equals("forever")) return null;

        String num = d.replaceAll("[^0-9]", "");
        String unit = d.replaceAll("[0-9]", "").trim();

        if (num.isEmpty()) throw new IllegalArgumentException("Invalid duration: " + duration);

        int value = Integer.parseInt(num);
        return switch (unit) {
            case "h", "hour", "hours" -> LocalDateTime.now().plusHours(value);
            case "d", "day", "days" -> LocalDateTime.now().plusDays(value);
            case "w", "week", "weeks" -> LocalDateTime.now().plusWeeks(value);
            default -> throw new IllegalArgumentException("Invalid unit: " + unit);
        };
    }

    // dto from entity
    public static ModeratorAssignmentDTO fromEntity(PollModerator moderator) {
        ModeratorAssignmentDTO dto = new ModeratorAssignmentDTO();
        dto.moderatorEmail = moderator.getModeratorEmail();
        dto.expiresAt = moderator.getExpiresAt();
        dto.durationUnit = moderator.isForeverAccess() ? "FOREVER" : null;
        return dto;
    }

    // getters and setters
    public String getModeratorEmail() {
        return moderatorEmail;
    }

    public void setModeratorEmail(String moderatorEmail) {
        this.moderatorEmail = moderatorEmail;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }


    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
