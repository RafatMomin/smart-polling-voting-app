package onetoone.dashboard;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.Dashboard.controller.DashboardController;
import onetoone.Dashboard.dto.RecentActivity;
import onetoone.Dashboard.service.DashboardService;
import onetoone.Users.model.Users;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController controller;

    private Users user;

    @Before
    public void setup() {
        user = new Users();
        user.setEmailId("leylak@iastate.edu");
    }

    /*
     * DASHBOARD FEATURE TEST: polls-joined with invalid token.
     * if token is invalid and user is null, controller should return an error map.
     */
    @Test
    public void pollsJoined_invalidToken_returnsError() {
        when(dashboardService.getUserFromToken("badtoken")).thenReturn(null);

        Object result = controller.getTotalPollsJoined("badtoken");
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("Invalid or expired token", map.get("error"));
    }

    /*
     * DASHBOARD FEATURE TEST: polls-joined with valid token.
     * successful call should update stats and return the count_polls_joined field.
     */
    @Test
    public void pollsJoined_validToken_returnsCount() {
        when(dashboardService.getUserFromToken("token")).thenReturn(user);
        when(dashboardService.getTotalPollsJoined("leylak@iastate.edu")).thenReturn(7L);

        Object result = controller.getTotalPollsJoined("token");
        Map<String, Object> map = (Map<String, Object>) result;

        verify(dashboardService).updateDashboardStatsForUser(user);
        assertEquals(7L, map.get("count_polls_joined"));
    }

    /*
     * DASHBOARD FEATURE TEST: votes-cast endpoint.
     * checks that the correct key count_votes_cast is returned.
     */
    @Test
    public void votesCast_validToken_returnsCount() {
        when(dashboardService.getUserFromToken("token")).thenReturn(user);
        when(dashboardService.getTotalVotesCast("leylak@iastate.edu")).thenReturn(3L);

        Object result = controller.getTotalVotesCast("token");
        Map<String, Object> map = (Map<String, Object>) result;

        assertEquals(3L, map.get("count_votes_cast"));
    }

    /*
     * DASHBOARD FEATURE TEST: poll-types endpoint.
     * returns whatever the service returns after updating stats.
     */
    @Test
    public void pollTypes_validToken_returnsBreakdown() {
        when(dashboardService.getUserFromToken("token")).thenReturn(user);
        when(dashboardService.getPollTypeBreakdown("leylak@iastate.edu"))
                .thenReturn(Map.of("yes_no", 2L));

        Object result = controller.getPollTypeBreakdown("token");
        Map<String, Long> map = (Map<String, Long>) result;

        assertEquals(Long.valueOf(2L), map.get("yes_no"));
    }

    /*
     * DASHBOARD FEATURE TEST: recent-activity endpoint.
     * returns list of RecentActivity from the service.
     */
    @Test
    public void recentActivity_validToken_returnsItems() {
        when(dashboardService.getUserFromToken("token")).thenReturn(user);
        RecentActivity a = new RecentActivity("Poll 1", "joined", java.time.LocalDateTime.now());
        when(dashboardService.getRecentActivity("leylak@iastate.edu"))
                .thenReturn(List.of(a));

        Object result = controller.getRecentActivity("token");
        List<RecentActivity> list = (List<RecentActivity>) result;

        assertEquals(1, list.size());
        assertEquals("Poll 1", list.get(0).getPollName());
    }
}
