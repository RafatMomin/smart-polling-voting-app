package onetoone.Polling.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "rating_polls")
public class RatingPoll extends Poll {

    public RatingPoll() { super(); }
    public RatingPoll(String title) { super(title); }

    @Override
    public String getTextRepresentation() {
        return super.getTextRepresentation();   // nothing extra
    }

    @Override
    public void validateVote(Vote vote) {
        Integer n = vote.getNumericValue();
        if (n == null || n < 1 || n > 5) {
            throw new IllegalArgumentException("RatingPoll requires numericValue between 1 and 5.");
        }
    }

    @Override
    public String getType() { return "RATING"; }
}