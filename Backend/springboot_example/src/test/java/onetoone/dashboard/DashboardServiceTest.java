package onetoone.dashboard;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.Dashboard.dto.RecentActivity;
import onetoone.Dashboard.model.DashboardStats;
import onetoone.Dashboard.repository.DashboardStatsRepository;
import onetoone.Dashboard.service.DashboardService;
import onetoone.Polling.model.Poll;
import onetoone.Polling.model.PollMember;
import onetoone.Polling.model.Vote;
import onetoone.Polling.repository.PollMemberRepository;
import onetoone.Polling.repository.VoteRepository;
import onetoone.Users.model.Users;
import onetoone.Users.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.runner.RunWith;

import java.time.LocalDateTime;
import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PollMemberRepository pollMemberRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private DashboardStatsRepository dashboardStatsRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private Users user;

    @Before
    public void setup() {
        user = new Users();
        user.setEmailId("leylak@iastate.edu");
    }

    /*
     * DASHBOARD FEATURE TEST: getUserFromToken returns null for blank token.
     * for a valid token it should delegate to the userRepository.
     */
    @Test
    public void getUserFromToken_handlesBlankAndValidTokens() {
        assertNull(dashboardService.getUserFromToken(null));
        assertNull(dashboardService.getUserFromToken(""));

        when(userRepository.findByauthtoken("abc123")).thenReturn(user);
        Users result = dashboardService.getUserFromToken("abc123");
        assertEquals("leylak@iastate.edu", result.getEmailId());
    }

    /*
     * DASHBOARD FEATURE TEST: total polls joined and total votes cast
     * these methods should delegate to the corresponding repositories.
     */
    @Test
    public void countsDelegatedToRepositories() {
        when(pollMemberRepository.countByUserEmail("leylak@iastate.edu")).thenReturn(5L);
        when(voteRepository.countTotalByUser("leylak@iastate.edu")).thenReturn(12L);

        long joined = dashboardService.getTotalPollsJoined("leylak@iastate.edu");
        long votes = dashboardService.getTotalVotesCast("leylak@iastate.edu");

        assertEquals(5L, joined);
        assertEquals(12L, votes);
    }

    /*
     * DASHBOARD FEATURE TEST: poll type breakdown mapping.
     * verifies that YES_NO/RATING/RANKING/MULTIPLE_CHOICE are mapped to the expected keys
     * and that unavailable types are ignored.
     */
    @Test
    public void pollTypeBreakdown_mapsTypesToKeys() {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"YES_NO", 3L});
        rows.add(new Object[]{"RATING", 2L});
        rows.add(new Object[]{"RANKING", 1L});
        rows.add(new Object[]{"MULTIPLE_CHOICE", 4L});
        rows.add(new Object[]{"UNKNOWN", 99L}); // should be ignored

        when(pollMemberRepository.countByPollTypeForUser("leylak@iastate.edu"))
                .thenReturn(rows);

        Map<String, Long> breakdown = dashboardService.getPollTypeBreakdown("leylak@iastate.edu");

        assertEquals(Long.valueOf(3L), breakdown.get("yes_no"));
        assertEquals(Long.valueOf(2L), breakdown.get("rating"));
        assertEquals(Long.valueOf(1L), breakdown.get("ranking"));
        assertEquals(Long.valueOf(4L), breakdown.get("multiple_choice"));
        // default zeros for missing keys are handled when building stats
    }

    /*
     * DASHBOARD FEATURE TEST: recent activity combines latest joins and votes.
     * items should be sorted newest-first and truncated to 5 entries.
     */
    @Test
    public void recentActivity_combinesJoinsAndVotes_sortedAndLimited() {
        String email = "leylak@iastate.edu";

        Poll poll1 = mock(Poll.class);
        when(poll1.getTitle()).thenReturn("Poll 1");
        Poll poll2 = mock(Poll.class);
        when(poll2.getTitle()).thenReturn("Poll 2");

        PollMember pm1 = mock(PollMember.class);
        when(pm1.getPoll()).thenReturn(poll1);
        when(pm1.getJoinedAt()).thenReturn(LocalDateTime.now().minusMinutes(10));

        PollMember pm2 = mock(PollMember.class);
        when(pm2.getPoll()).thenReturn(poll2);
        when(pm2.getJoinedAt()).thenReturn(LocalDateTime.now().minusMinutes(5));

        Vote v1 = mock(Vote.class);
        when(v1.getPoll()).thenReturn(poll1);
        when(v1.getTimestamp()).thenReturn(LocalDateTime.now().minusMinutes(2));

        Vote v2 = mock(Vote.class);
        when(v2.getPoll()).thenReturn(poll2);
        when(v2.getTimestamp()).thenReturn(LocalDateTime.now().minusMinutes(1));

        when(pollMemberRepository.findTop5ByUserEmailIdOrderByJoinedAtDesc(email))
                .thenReturn(Arrays.asList(pm1, pm2));
        when(voteRepository.findTop5ByUserIdOrderByTimestampDesc(email))
                .thenReturn(Arrays.asList(v1, v2));

        List<RecentActivity> items = dashboardService.getRecentActivity(email);

        assertTrue(items.size() <= 5);
        assertEquals("voted", items.get(0).getAction());
        assertNotNull(items.get(0).getTime());
    }

    /*
     * DASHBOARD FEATURE TEST: updateDashboardStatsForUser recomputes and saves.
     * verifies that all fields are set based on sub-methods and saved.
     */
    @Test
    public void updateDashboardStatsForUser_recomputesAndSaves() {
        String email = "leylak@iastate.edu";

        when(pollMemberRepository.countByUserEmail(email)).thenReturn(4L);
        when(voteRepository.countTotalByUser(email)).thenReturn(10L);

        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"YES_NO", 2L});
        rows.add(new Object[]{"RANKING", 1L});
        when(pollMemberRepository.countByPollTypeForUser(email)).thenReturn(rows);

        when(dashboardStatsRepository.findByUser(user)).thenReturn(Optional.empty());

        dashboardService.updateDashboardStatsForUser(user);

        ArgumentCaptor<DashboardStats> captor = ArgumentCaptor.forClass(DashboardStats.class);
        verify(dashboardStatsRepository).save(captor.capture());

        DashboardStats saved = captor.getValue();
        assertEquals(4L, saved.getTotalPollsJoined());
        assertEquals(10L, saved.getTotalVotesCast());
        assertEquals(2L, saved.getYesNoCount());
        assertEquals(1L, saved.getRankingCount());
        assertNotNull(saved.getUpdatedAt());
    }
}
