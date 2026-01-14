package onetoone.PollingWindow.dto;

import onetoone.PollingWindow.model.Group;
import java.util.Set;

/**
 * DTO for Group responses - matches API documentation format
 */
public class GroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String creatorEmail;
    private Set<String> memberEmails;

    public static GroupResponseDTO fromEntity(Group group) {
        GroupResponseDTO dto = new GroupResponseDTO();
        dto.id = group.getId();
        dto.name = group.getName();
        dto.description = group.getDescription();
        dto.creatorEmail = group.getCreatorEmail();
        dto.memberEmails = group.getMemberEmails();
        return dto;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatorEmail() { return creatorEmail; }
    public void setCreatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; }

    public Set<String> getMemberEmails() { return memberEmails; }
    public void setMemberEmails(Set<String> memberEmails) { this.memberEmails = memberEmails; }
}