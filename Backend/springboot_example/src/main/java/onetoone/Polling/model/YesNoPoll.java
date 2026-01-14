package onetoone.Polling.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "yes_no_polls")
public class YesNoPoll extends Poll {

    public YesNoPoll() { super(); }
    public YesNoPoll(String title) { super(title); }

    @Override
    public String getTextRepresentation() {
        return super.getTextRepresentation();   // nothing extra
    }

    @Override
    public void validateVote(Vote vote) {
        // use numericValue: 0 = no, 1 = yes
        Integer n = vote.getNumericValue();
        if (n == null || (n != 0 && n != 1)) {
            throw new IllegalArgumentException("YesNoPoll requires numericValue of 0 (No) or 1 (Yes).");
        }
    }

    @Override
    public String getType() {
        return "YES_NO";
    }
}