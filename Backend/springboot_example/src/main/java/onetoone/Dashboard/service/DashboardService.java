package onetoone.Dashboard.service;

import onetoone.Dashboard.dto.RecentActivity;
import onetoone.Dashboard.model.DashboardStats;
import onetoone.Dashboard.repository.DashboardStatsRepository;
import onetoone.Polling.model.PollMember;
import onetoone.Polling.model.Vote;
import onetoone.Polling.repository.PollMemberRepository;
import onetoone.Polling.repository.VoteRepository;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import java.util.Comparator;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PollMemberRepository pollMemberRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private DashboardStatsRepository dashboardStatsRepository;

    // get user id from auth token
    public Users getUserFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return userRepository.findByauthtoken(token);
    }

    // total polls joined
    public long getTotalPollsJoined(String userEmail) {
        return pollMemberRepository.countByUserEmail(userEmail);
    }

    // total votes cast
    public long getTotalVotesCast(String userEmail) {
        return voteRepository.countTotalByUser(userEmail);
    }

    // poll types user has joined
    public Map<String, Long> getPollTypeBreakdown(String userEmail) {
        List<Object[]> rows = pollMemberRepository.countByPollTypeForUser(userEmail);

        Map<String, Long> breakdown = new LinkedHashMap<>();
        breakdown.put("yes_no", 0L);
        breakdown.put("rating", 0L);
        breakdown.put("ranking", 0L);
        breakdown.put("multiple_choice", 0L);

        for (Object[] row : rows) {
            String type = (String) row[0];
            Long count = (Long) row[1];

            String key = mapPollType(type);
            if (key != null) {
                breakdown.put(key, count);
            }
        }
        return breakdown;
    }

    // map poll type
    private String mapPollType(String type) {
        if (type == null) return null;
        switch (type.toUpperCase()) {
            case "YES_NO":
                return "yes_no";
            case "RATING":
                return "rating";
            case "RANKING":
                return "ranking";
            case "MULTIPLE_CHOICE":
                return "multiple_choice";
            default:
                return null;
        }
    }

    // last 5 polls joined aka recent activity
    public List<RecentActivity> getRecentActivity(String userEmail) {
        // last 5 joins
        List<PollMember> recentJoins =
                pollMemberRepository.findTop5ByUserEmailIdOrderByJoinedAtDesc(userEmail);

        // last 5 votes
        List<Vote> recentVotes =
                voteRepository.findTop5ByUserIdOrderByTimestampDesc(userEmail);

        List<RecentActivity> items = new ArrayList<>();

        for (PollMember pm : recentJoins) {
            if (pm.getPoll() == null) continue;
            items.add(new RecentActivity(
                    pm.getPoll().getTitle(),
                    "joined",
                    pm.getJoinedAt()
            ));
        }

        for (Vote v : recentVotes) {
            if (v.getPoll() == null) continue;
            items.add(new RecentActivity(
                    v.getPoll().getTitle(),
                    "voted",
                    v.getTimestamp()
            ));
        }

        // sort by timestamp descending and return top 5
        return items.stream()
                .filter(i -> i.getTime() != null)
                .sorted(Comparator.comparing(RecentActivity::getTime).reversed())
                .limit(5)
                .collect(Collectors.toList());

    }

    // recompute dashboard stats for a user and store them in dashboard_stats
    public void updateDashboardStatsForUser(Users user) {
        if (user == null) return;

        String email = user.getEmailId();

        long totalPollsJoined = getTotalPollsJoined(email);
        long totalVotesCast = getTotalVotesCast(email);

        Map<String, Long> breakdown = getPollTypeBreakdown(email);

        long yesNo = breakdown.getOrDefault("yes_no", 0L);
        long rating = breakdown.getOrDefault("rating", 0L);
        long ranking = breakdown.getOrDefault("ranking", 0L);
        long multipleChoice = breakdown.getOrDefault("multiple_choice", 0L);

        DashboardStats stats = dashboardStatsRepository
                .findByUser(user)
                .orElse(new DashboardStats(user));

        stats.setTotalPollsJoined(totalPollsJoined);
        stats.setTotalVotesCast(totalVotesCast);
        stats.setYesNoCount(yesNo);
        stats.setRatingCount(rating);
        stats.setRankingCount(ranking);
        stats.setMultipleChoiceCount(multipleChoice);
        stats.setUpdatedAt(java.time.LocalDateTime.now());

        dashboardStatsRepository.save(stats);
    }

}
