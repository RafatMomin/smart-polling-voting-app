package onetoone.LiveResults.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class LiveResultsMessage {

    @JsonProperty("pollId")
    private Long pollId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("totalVotes")
    private Integer totalVotes;

    @JsonProperty("distribution")
    private List<OptionDistribution> distribution;

    public LiveResultsMessage() {}

    public LiveResultsMessage(Long pollId, String status, Integer totalVotes, List<OptionDistribution> distribution) {
        this.pollId = pollId;
        this.status = status;
        this.totalVotes = totalVotes;
        this.distribution = distribution;
    }

    // getters and setters for poll stuff
    public Long getPollId() {
        return pollId;
    }

    public void setPollId(Long pollId) {
        this.pollId = pollId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public List<OptionDistribution> getDistribution() {
        return distribution;
    }

    public void setDistribution(List<OptionDistribution> distribution) {
        this.distribution = distribution;
    }

    public static class OptionDistribution {
        @JsonProperty("optionId")
        private Long optionId;

        @JsonProperty("count")
        private Integer count;

        @JsonProperty("percent")
        private Double percent;

        public OptionDistribution() {}

        public OptionDistribution(Long optionId, Integer count, Double percent) {
            this.optionId = optionId;
            this.count = count;
            this.percent = percent;
        }

        // getters and setters for options and data
        public Long getOptionId() {
            return optionId;
        }

        public void setOptionId(Long optionId) {
            this.optionId = optionId;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public Double getPercent() {
            return percent;
        }

        public void setPercent(Double percent) {
            this.percent = percent;
        }
    }
}