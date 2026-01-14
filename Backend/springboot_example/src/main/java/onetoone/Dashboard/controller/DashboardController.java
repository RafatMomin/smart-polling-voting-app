package onetoone.Dashboard.controller;

import onetoone.Dashboard.service.DashboardService;
import onetoone.Users.model.Users;
import onetoone.Dashboard.dto.RecentActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // helper method for an invalid token reponse
    private Map<String, Object> invalidToken() {
        Map<String, Object> map = new HashMap<>();
        map.put("error", "Invalid or expired token");
        return map;
    }

    //total polls joined by a user
    @GetMapping("/polls-joined")
    public Object getTotalPollsJoined(@RequestHeader("Authorization") String token) {

        Users user = dashboardService.getUserFromToken(token);
        if (user == null) return invalidToken();

        dashboardService.updateDashboardStatsForUser(user);

        Map<String, Object> map = new HashMap<>();
        map.put("count_polls_joined", dashboardService.getTotalPollsJoined(user.getEmailId()));
        return map;
    }

    // total votes cast by a user
    @GetMapping("/votes-cast")
    public Object getTotalVotesCast(@RequestHeader("Authorization") String token) {

        Users user = dashboardService.getUserFromToken(token);
        if (user == null) return invalidToken();

        dashboardService.updateDashboardStatsForUser(user);

        Map<String, Object> map = new HashMap<>();
        map.put("count_votes_cast", dashboardService.getTotalVotesCast(user.getEmailId()));
        return map;
    }

    // poll types a user has joined
    @GetMapping("/poll-types")
    public Object getPollTypeBreakdown(@RequestHeader("Authorization") String token) {

        Users user = dashboardService.getUserFromToken(token);
        if (user == null) return invalidToken();

        dashboardService.updateDashboardStatsForUser(user);

        return dashboardService.getPollTypeBreakdown(user.getEmailId());
    }

    // recent activity
    @GetMapping("/recent-activity")
    public Object getRecentActivity(@RequestHeader("Authorization") String token) {

        Users user = dashboardService.getUserFromToken(token);
        if (user == null) return invalidToken();

        dashboardService.updateDashboardStatsForUser(user);

        List<RecentActivity> items =
                dashboardService.getRecentActivity(user.getEmailId());

        return items;
    }
}
