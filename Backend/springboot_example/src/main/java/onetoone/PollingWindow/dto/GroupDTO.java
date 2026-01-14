package onetoone.PollingWindow.dto;

import onetoone.PollingWindow.model.Group;

import java.util.Set;

public class GroupDTO {
    private Long id;
    private String name;
    private String description;
    private String creatorEmail;
    private Set<String> memberEmails;

    public static GroupDTO fromEntity(Group group) {
        if (group == null) return null;
        GroupDTO dto = new GroupDTO();
        dto.id = group.getId();
        dto.name = group.getName();
        dto.description = group.getDescription();
        dto.creatorEmail = group.getCreatorEmail();
        dto.memberEmails = group.getMemberEmails();
        return dto;
    }

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

