package onetoone.LiveDiscussion.dto;

import java.util.List;

public class CreateGeneralDiscussionRequest {
    private String title;
    private String description;
    private List<String> tags;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}