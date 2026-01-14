package onetoone.Polling.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "multiple_choice_polls")
public class MultipleChoicePoll extends Poll {

    @ElementCollection
    private List<String> options = new ArrayList<>();

    public MultipleChoicePoll() { super(); }
    public MultipleChoicePoll(String title, List<String> options) {
        super(title);
        this.options = options;
    }

    // MultipleChoicePoll.java
    @Override
    public String getTextRepresentation() {
        String opts = options != null ? options.toString() : "[]";
        return super.getTextRepresentation() + ", Options: " + opts;
    }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    @Override
    public void validateVote(Vote vote) {
        String choice = vote.getChoice();
        if (choice == null || !options.contains(choice)) {
            throw new IllegalArgumentException("MultipleChoicePoll requires choice to be one of configured options: " + options);
        }
    }

    @Override
    public String getType() { return "MULTIPLE_CHOICE"; }
}