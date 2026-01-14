package onetoone.PollingWindow.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import onetoone.Users.model.Users;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group of users (e.g., "Section 3", "Class A")
 * Many-to-Many relationship with Users
 */
@Entity
@Table(name = "user_groups")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    // Many-to-One: Creator/administrator of the group
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_email", referencedColumnName = "emailId")
    @JsonIgnore
    private Users creator;

    // Many-to-Many: A group can have many users, a user can be in many groups
    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_email", referencedColumnName = "emailId")
    )
    @JsonIgnore
    private Set<Users> members = new HashSet<>();

    public Group() {}

    public Group(String name, Users creator) {
        this.name = name;
        this.creator = creator;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Users getCreator() { return creator; }
    public void setCreator(Users creator) { this.creator = creator; }

    // Convenience method to get creator email
    public String getCreatorEmail() {
        return creator != null ? creator.getEmailId() : null;
    }

    public Set<Users> getMembers() { return members; }
    public void setMembers(Set<Users> members) { this.members = members; }

    // Convenience method to get member emails
    public Set<String> getMemberEmails() {
        Set<String> emails = new HashSet<>();
        for (Users user : members) {
            emails.add(user.getEmailId());
        }
        return emails;
    }

    public void addMember(Users user) {
        this.members.add(user);
    }

    public void removeMember(Users user) {
        this.members.remove(user);
    }

    // Check if a user is a member by email
    public boolean isMember(String email) {
        return members.stream()
                .anyMatch(user -> user.getEmailId().equals(email));
    }
}