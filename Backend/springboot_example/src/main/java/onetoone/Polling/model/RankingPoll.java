package onetoone.Polling.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ranking_polls")
public class RankingPoll extends Poll {

    @ElementCollection
    private List<String> options = new ArrayList<>();

    public RankingPoll() { super(); }
    public RankingPoll(String title, List<String> options) {
        super(title);
        this.options = options;
    }


    // RankingPoll.java
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
        Integer rank = vote.getRank();
        if (choice == null || !options.contains(choice)) {
            throw new IllegalArgumentException("RankingPoll requires choice to be a valid option: " + options);
        }
        if (rank == null || rank < 1 || rank > options.size()) {
            throw new IllegalArgumentException("RankingPoll requires rank between 1 and " + options.size());
        }
    }

    @Override
    public String getType() { return "RANKING"; }
}
