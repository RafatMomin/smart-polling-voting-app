package onetoone.Users.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import onetoone.AI.model.ChatMessage;
import onetoone.Polling.model.PollModerator;
import onetoone.PollingWindow.model.Group;
import onetoone.PollingWindow.model.PollingWindow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Rohan Sah
 *
 */

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Users {

    @Id
    private String emailId;
    private String name;
    private String password;
    private boolean isVerified;
    private String verificationToken;
    private String resetToken;
    private String authtoken;

    // One-to-Many relationship with ChatMessages
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ChatMessage> chatMessages = new ArrayList<>();

    // One-to-Many: User can create many groups
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Group> createdGroups = new HashSet<>();

    // Many-to-Many: User can be member of many groups
    @ManyToMany(mappedBy = "members")
    @JsonIgnore
    private Set<Group> memberOfGroups = new HashSet<>();

    // One-to-Many: User can create many polling windows
    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<PollingWindow> createdPollingWindows = new HashSet<>();

    // Many-to-Many: User can be individually targeted by many polling windows
    @ManyToMany(mappedBy = "targetIndividualUsers")
    @JsonIgnore
    private Set<PollingWindow> targetedPollingWindows = new HashSet<>();

    // ONE-TO-MANY relationship: user can be moderator of many polls
    @OneToMany(mappedBy = "moderator", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<PollModerator> moderatedPolls = new HashSet<>();

    // Constructors
    public Users(String name, String emailId, String password) {
        this.name = name;
        this.emailId = emailId;
        this.password = password;
    }

    public Users() {
    }

    // Existing getters and setters
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getEmailId(){
        return emailId;
    }

    public void setEmailId(String emailId){
        this.emailId = emailId;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getAuthtoken() {
        return authtoken;
    }

    public void setAuthtoken(String authtoken) {
        this.authtoken = authtoken;
    }

    // ChatMessage relationship getters/setters
    public List<ChatMessage> getChatMessages() {
        return chatMessages;
    }

    public void setChatMessages(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
    }

    public void addChatMessage(ChatMessage message) {
        chatMessages.add(message);
        message.setUser(this);
    }

    // Group relationship getters/setters
    public Set<Group> getCreatedGroups() {
        return createdGroups;
    }

    public void setCreatedGroups(Set<Group> createdGroups) {
        this.createdGroups = createdGroups;
    }

    public Set<Group> getMemberOfGroups() {
        return memberOfGroups;
    }

    public void setMemberOfGroups(Set<Group> memberOfGroups) {
        this.memberOfGroups = memberOfGroups;
    }

    // PollingWindow relationship getters/setters
    public Set<PollingWindow> getCreatedPollingWindows() {
        return createdPollingWindows;
    }

    public void setCreatedPollingWindows(Set<PollingWindow> createdPollingWindows) {
        this.createdPollingWindows = createdPollingWindows;
    }

    public Set<PollingWindow> getTargetedPollingWindows() {
        return targetedPollingWindows;
    }

    public void setTargetedPollingWindows(Set<PollingWindow> targetedPollingWindows) {
        this.targetedPollingWindows = targetedPollingWindows;
    }

    public Set<PollModerator> getModeratedPolls() {
        return moderatedPolls;
    }

    public void setModeratedPolls(Set<PollModerator> moderatedPolls) {
        this.moderatedPolls = moderatedPolls;
    }
}