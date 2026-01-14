package onetoone.Dashboard.model;

import jakarta.persistence.*;
import onetoone.Users.model.Users;

import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_stats", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"emailId"})
})
public class DashboardStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emailId", referencedColumnName = "emailId", nullable = false)
    private Users user;

    private long totalPollsJoined;
    private long totalVotesCast;

    private long yesNoCount;
    private long ratingCount;
    private long rankingCount;
    private long multipleChoiceCount;

    private LocalDateTime updatedAt;

    public DashboardStats() {}

    public DashboardStats(Users user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public Users getUser() {
        return user;
    }
    public void setUser(Users user) {
        this.user = user;
    }

    public long getTotalPollsJoined() {
        return totalPollsJoined;
    }
    public void setTotalPollsJoined(long totalPollsJoined) {
        this.totalPollsJoined = totalPollsJoined;
    }

    public long getTotalVotesCast() {
        return totalVotesCast;
    }
    public void setTotalVotesCast(long totalVotesCast) {
        this.totalVotesCast = totalVotesCast;
    }

    public long getYesNoCount() {
        return yesNoCount;
    }
    public void setYesNoCount(long yesNoCount) {
        this.yesNoCount = yesNoCount;
    }

    public long getRatingCount() {
        return ratingCount;
    }
    public void setRatingCount(long ratingCount) {
        this.ratingCount = ratingCount;
    }

    public long getRankingCount() {
        return rankingCount;
    }
    public void setRankingCount(long rankingCount) {
        this.rankingCount = rankingCount;
    }

    public long getMultipleChoiceCount() {
        return multipleChoiceCount;
    }
    public void setMultipleChoiceCount(long multipleChoiceCount) {
        this.multipleChoiceCount = multipleChoiceCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
