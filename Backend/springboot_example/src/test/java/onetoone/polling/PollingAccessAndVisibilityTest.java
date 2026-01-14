package onetoone.polling;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.Location.dto.VoteRequest;
import onetoone.Location.service.LocationService;
import onetoone.LiveResults.service.LiveResultsService;
import onetoone.Polling.controller.PollController;
import onetoone.Polling.dto.ModeratorAssignmentDTO;
import onetoone.Polling.model.*;
import onetoone.Polling.repository.PollRepository;
import onetoone.Polling.service.PollService;
import onetoone.Users.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import java.util.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class PollingAccessAndVisibilityTest {

    @Mock
    private PollService pollService;

    @Mock
    private PollRepository pollRepository;

    @Mock
    private LocationService locationService;

    @Mock
    private LiveResultsService liveResultsService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PollController controller;

    private Poll publicPoll;
    private Poll privatePoll;

    @Before
    public void setup() {
        publicPoll = mock(Poll.class);
        when(publicPoll.getId()).thenReturn(1L);
        when(publicPoll.getVisibility()).thenReturn(Poll.Visibility.PUBLIC);
        when(publicPoll.getType()).thenReturn("YES_NO");

        privatePoll = mock(Poll.class);
        when(privatePoll.getId()).thenReturn(2L);
        when(privatePoll.getVisibility()).thenReturn(Poll.Visibility.PRIVATE);
        when(privatePoll.getType()).thenReturn("RATING");
    }

    /* ------------------------------------------------------------*/
    /*  getAllPolls / getPoll VISIBILITY TESTS                      */
    /* ------------------------------------------------------------*/

    /*
     * POLLING VISIBILITY FEATURE TEST: getAllPolls without emailId
     * should only return public polls.
     */
    @Test
    public void getAllPolls_noEmail_returnsOnlyPublic() {
        when(pollRepository.findAll()).thenReturn(Arrays.asList(publicPoll, privatePoll));

        List<Poll> result = controller.getAllPolls(null, null, null);

        assertEquals(1, result.size());
        assertEquals(Poll.Visibility.PUBLIC, result.get(0).getVisibility());
    }

    /*
     * POLLING VISIBILITY FEATURE TEST: getAllPolls with emailId
     * should include public polls plus private polls where user is owner or member.
     */
    @Test
    public void getAllPolls_withEmail_includesPrivateMembership() {
        when(pollRepository.findAll()).thenReturn(Arrays.asList(publicPoll, privatePoll));
        when(pollService.isOwner(2L, "user@x.com")).thenReturn(false);
        when(pollService.isMember(2L, "user@x.com")).thenReturn(true);

        List<Poll> result = controller.getAllPolls("user@x.com", null, null);

        assertEquals(2, result.size());
    }

    /*
     * POLLING VISIBILITY FEATURE TEST: getPoll returns error if poll not found.
     */
    @Test
    public void getPoll_notFound_returnsError() {
        when(pollService.findPoll(99L)).thenReturn(Optional.empty());

        Object result = controller.getPoll(99L, null);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("Poll not found", map.get("error"));
    }

    /*
     * POLLING VISIBILITY FEATURE TEST: private poll without emailId
     * should return error asking for emailId.
     */
    @Test
    public void getPoll_privateMissingEmail_returnsError() {
        when(pollService.findPoll(2L)).thenReturn(Optional.of(privatePoll));

        Object result = controller.getPoll(2L, null);
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("emailId required for private polls", map.get("error"));
    }

    /*
     * POLLING VISIBILITY FEATURE TEST: private poll non-member
     * should deny access.
     */
    @Test
    public void getPoll_privateNonMember_denied() {
        when(pollService.findPoll(2L)).thenReturn(Optional.of(privatePoll));
        when(pollService.isOwner(2L, "user@x.com")).thenReturn(false);
        when(pollService.isMember(2L, "user@x.com")).thenReturn(false);

        Object result = controller.getPoll(2L, "user@x.com");
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("Access denied. Not a member of this poll.", map.get("error"));
    }

    /*
     * POLLING VISIBILITY FEATURE TEST: private poll member
     * should return the poll.
     */
    @Test
    public void getPoll_privateMember_allowed() {
        when(pollService.findPoll(2L)).thenReturn(Optional.of(privatePoll));
        when(pollService.isOwner(2L, "member@x.com")).thenReturn(true);
        when(pollService.isMember(2L, "member@x.com")).thenReturn(true);

        Object result = controller.getPoll(2L, "member@x.com");
        assertSame(privatePoll, result);
    }

    /* ------------------------------------------------------------*/
    /*  JOIN / ACCESS CODE ENDPOINTS                               */
    /* ------------------------------------------------------------*/

    /*
     * POLLING ACCESS FEATURE TEST: joinPoll with missing fields
     * returns appropriate error messages.
     */
    @Test
    public void joinPoll_missingFields_returnsErrors() {
        Map<String, String> body = new HashMap<>();
        body.put("emailId", "user@x.com");

        Map<String, Object> r1 = controller.joinPoll(body);
        assertEquals("Access code required", r1.get("error"));

        body.clear();
        body.put("code", "ABC123");
        Map<String, Object> r2 = controller.joinPoll(body);
        assertEquals("emailId required", r2.get("error"));
    }

    /*
     * POLLING ACCESS FEATURE TEST: joinPoll with valid data
     * should return poll info and delegate to PollService.
     */
    @Test
    public void joinPoll_valid_returnsPollInfo() {
        Map<String, String> body = new HashMap<>();
        body.put("code", "ABC123");
        body.put("emailId", "user@x.com");

        Poll poll = mock(Poll.class);
        when(poll.getId()).thenReturn(10L);
        when(poll.getTitle()).thenReturn("My Poll");
        when(poll.getVisibility()).thenReturn(Poll.Visibility.PRIVATE);

        when(pollService.joinPoll("ABC123", "user@x.com")).thenReturn(poll);

        Map<String, Object> res = controller.joinPoll(body);

        assertEquals(10L, res.get("pollId"));
        assertEquals("My Poll", res.get("title"));
        assertEquals("PRIVATE", res.get("visibility"));
    }

    /*
     * POLLING ACCESS FEATURE TEST: rotateAccessCode missing emailId.
     */
    @Test
    public void rotateAccessCode_missingEmail_returnsError() {
        Map<String, String> body = new HashMap<>();
        ResponseEntity<?> r; // to avoid confusion if changed, we check current type

        Map<String, String> res = controller.rotateAccessCode(1L, body);
        assertEquals("emailId required", res.get("error"));
    }

    /*
     * POLLING ACCESS FEATURE TEST: getAccessCode + rotateAccessCode valid.
     */
    @Test
    public void accessCode_happyPaths() {
        when(pollService.rotateAccessCode(1L, "owner@x.com")).thenReturn("NEW123");
        Map<String, String> rotateRes = controller.rotateAccessCode(1L, Map.of("emailId", "owner@x.com"));
        assertEquals("NEW123", rotateRes.get("accessCode"));

        when(pollService.getAccessCode(1L, "owner@x.com")).thenReturn("CODE1");
        Map<String, String> getRes = controller.getAccessCode(1L, "owner@x.com");
        assertEquals("CODE1", getRes.get("accessCode"));
    }

    /* ------------------------------------------------------------*/
    /*  RESULTS / VOTE + LOCATION & VISIBILITY                     */
    /* ------------------------------------------------------------*/

    /*
     * POLLING RESULTS FEATURE TEST: getResults private poll non-member denied.
     */
    @Test
    public void getResults_privateNonMember_denied() {
        when(privatePoll.getType()).thenReturn("YES_NO");
        when(pollService.findPoll(2L)).thenReturn(Optional.of(privatePoll));
        when(pollService.isOwner(2L, "stranger@x.com")).thenReturn(false);
        when(pollService.isMember(2L, "stranger@x.com")).thenReturn(false);

        Object result = controller.getResults(2L, "stranger@x.com");
        Map<String, Object> map = (Map<String, Object>) result;
        assertEquals("Access denied, the user is not a member of this poll.", map.get("error"));
    }

    /*
     * POLLING RESULTS FEATURE TEST: getResults YES_NO public poll.
     * should delegate to pollService.getYesNoStats.
     */
    @Test
    public void getResults_yesNoPublic_delegatesToService() {
        when(publicPoll.getType()).thenReturn("YES_NO");
        when(pollService.findPoll(1L)).thenReturn(Optional.of(publicPoll));
        when(pollService.getYesNoStats(1L)).thenReturn(Map.of("yes", 3, "no", 1));

        Object result = controller.getResults(1L, null);
        Map<String, Object> map = (Map<String, Object>) result;

        assertEquals(3, map.get("yes"));
        assertEquals(1, map.get("no"));
    }

    /*
     * POLLING VOTE FEATURE TEST: private poll non-member
     * should return an error and not validate location or add vote.
     */
    @Test
    public void vote_privateNonMember_returnsError() {
        when(pollService.findPoll(2L)).thenReturn(Optional.of(privatePoll));
        when(pollService.isOwner(2L, "user@x.com")).thenReturn(false);
        when(pollService.isMember(2L, "user@x.com")).thenReturn(false);

        VoteRequest req = new VoteRequest();
        req.setLat(42.0);
        req.setLng(-93.0);

        Map<String, Object> res = controller.vote(2L, "user@x.com", req, 1, null);

        assertEquals("Access denied, the user must be a member of this poll to vote on private polls.",
                res.get("error"));
        verify(locationService, never()).validateLocation(anyDouble(), anyDouble());
        verify(pollService, never()).addVote(anyLong(), any(Vote.class));
    }

    /*
     * POLLING VOTE FEATURE TEST: valid public poll vote with numeric value.
     * should validate location, add vote, and broadcast live results.
     */
    @Test
    public void vote_publicPoll_numericValue_addsVoteAndBroadcasts() {
        when(pollService.findPoll(1L)).thenReturn(Optional.of(publicPoll));

        VoteRequest req = new VoteRequest();
        req.setLat(42.0);
        req.setLng(-93.0);

        Map<String, Object> res = controller.vote(1L, "user@x.com", req, 1, null);

        assertEquals("Vote added", res.get("message"));
        verify(locationService).validateLocation(42.0, -93.0);
        verify(pollService).addVote(eq(1L), any(Vote.class));
        verify(liveResultsService).broadcastResults(1L);
    }

    /*
     * POLLING VOTE FEATURE TEST: vote invalid payload without numericValue or choice.
     * should return error message.
     */
    @Test
    public void vote_invalidPayload_returnsError() {
        when(pollService.findPoll(1L)).thenReturn(Optional.of(publicPoll));

        VoteRequest req = new VoteRequest();
        req.setLat(42.0);
        req.setLng(-93.0);

        Map<String, Object> res = controller.vote(1L, "user@x.com", req, null, null);

        assertTrue(res.get("error").toString().contains("Invalid vote payload"));
    }

    /*
     * POLLING RANKING FEATURE TEST: private ranking poll non-member cannot vote.
     */
    @Test
    public void submitRanking_privateNonMember_forbidden() {
        RankingPoll rp = mock(RankingPoll.class);
        when(rp.getOptions()).thenReturn(Arrays.asList("A", "B"));
        when(pollService.findPoll(2L)).thenReturn(Optional.of(rp));
        when(rp.getVisibility()).thenReturn(Poll.Visibility.PRIVATE);
        when(rp.getType()).thenReturn("RANKING");

        List<VoteRequest> list = new ArrayList<>();
        VoteRequest vr = new VoteRequest();
        vr.setChoice("A");
        vr.setRank(1);
        list.add(vr);

        ResponseEntity<?> response = controller.submitRanking(2L, "user@x.com", list);
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertTrue(body.get("error").toString()
                .contains("Access denied, user must be a member"));
    }

    /*
     * POLLING RANKING FEATURE TEST: submitRanking valid list
     * should validate location, add ranking votes, and broadcast results.
     */
    @Test
    public void submitRanking_valid_callsServiceAndBroadcasts() {
        RankingPoll rp = mock(RankingPoll.class);
        when(rp.getOptions()).thenReturn(Arrays.asList("A", "B"));
        when(rp.getVisibility()).thenReturn(Poll.Visibility.PUBLIC);
        when(pollService.findPoll(5L)).thenReturn(Optional.of(rp));

        VoteRequest v1 = new VoteRequest();
        v1.setChoice("A");
        v1.setRank(1);
        v1.setLat(42.0);
        v1.setLng(-93.0);

        VoteRequest v2 = new VoteRequest();
        v2.setChoice("B");
        v2.setRank(2);
        v2.setLat(42.0);
        v2.setLng(-93.0);

        List<VoteRequest> list = Arrays.asList(v1, v2);

        ResponseEntity<?> res = controller.submitRanking(5L, "user@x.com", list);
        Map<String, Object> body = (Map<String, Object>) res.getBody();

        assertEquals("Ranking submitted", body.get("message"));
        verify(locationService).validateLocation(42.0, -93.0);
        verify(pollService).addRankingVotes(eq(5L), anyList());
        verify(liveResultsService).broadcastResults(5L);
    }

    /* ------------------------------------------------------------*/
    /*  MODERATOR & ACCESS LEVEL ENDPOINTS                          */
    /* ------------------------------------------------------------*/

    /*
     * MODERATOR FEATURE TEST: assignModerator happy path.
     * verifies that pollService.assignModerator is called and response contains moderator data.
     */
    @Test
    public void assignModerator_success_returnsModeratorInfo() {
        ModeratorAssignmentDTO dto = new ModeratorAssignmentDTO();
        dto.setModeratorEmail("mod@x.com");
        dto.setDuration("FOREVER");

        PollModerator pm = mock(PollModerator.class);
        when(pm.getModeratorEmail()).thenReturn("mod@x.com");
        when(pm.getExpiresAt()).thenReturn(null);
        when(pm.isForeverAccess()).thenReturn(true);

        when(pollService.assignModerator(1L, "owner@x.com", dto)).thenReturn(pm);

        ResponseEntity<?> res = controller.assignModerator(1L, "owner@x.com", dto);
        Map<String, Object> body = (Map<String, Object>) res.getBody();

        assertEquals("Moderator assigned successfully", body.get("message"));
        assertEquals("mod@x.com", body.get("moderatorEmail"));
    }

    /*
     * MODERATOR FEATURE TEST: removeModerator should call pollService.removeModerator.
     */
    @Test
    public void removeModerator_success_callsService() {
        ResponseEntity<?> res = controller.removeModerator(1L, "mod@x.com", "owner@x.com");

        assertEquals(200, res.getStatusCodeValue());
        verify(pollService).removeModerator(1L, "owner@x.com", "mod@x.com");
    }

    /*
     * MODERATOR FEATURE TEST: getPollModerators returns list from service.
     */
    @Test
    public void getPollModerators_returnsList() {
        PollModerator pm = mock(PollModerator.class);
        when(pm.getModeratorEmail()).thenReturn("mod@x.com");
        when(pm.getAssignedAt()).thenReturn(java.time.LocalDateTime.now());
        when(pm.getExpiresAt()).thenReturn(null);
        when(pm.isForeverAccess()).thenReturn(true);
        when(pm.isCurrentlyValid()).thenReturn(true);

        when(pollService.getPollModerators(1L, "owner@x.com"))
                .thenReturn(List.of(pm));

        ResponseEntity<?> res = controller.getPollModerators(1L, "owner@x.com");
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        List<?> list = (List<?>) body.get("moderators");

        assertEquals(1, list.size());
    }

    /*
     * ACCESS LEVEL FEATURE TEST: getUserAccessiblePolls delegates to service.
     */
    @Test
    public void getUserAccessiblePolls_delegatesToService() {
        when(pollService.getUserAccessiblePolls("user@x.com"))
                .thenReturn(List.of(Map.of("pollId", 1L)));

        ResponseEntity<?> res = controller.getUserAccessiblePolls("user@x.com");
        Map<String, Object> body = (Map<String, Object>) res.getBody();
        List<?> list = (List<?>) body.get("polls");

        assertEquals(1, list.size());
    }

    /*
     * ACCESS LEVEL FEATURE TEST: checkAccessLevel builds role and flags from service booleans.
     */
    @Test
    public void checkAccessLevel_setsRoleAndFlags() {
        when(pollService.isPollOwner(1L, "owner@x.com")).thenReturn(true);
        when(pollService.isPollModerator(1L, "owner@x.com")).thenReturn(false);
        when(pollService.canEditPoll(1L, "owner@x.com")).thenReturn(true);
        when(pollService.canDeletePoll(1L, "owner@x.com")).thenReturn(true);

        ResponseEntity<?> res = controller.checkAccessLevel(1L, "owner@x.com");
        Map<String, Object> body = (Map<String, Object>) res.getBody();

        assertEquals("OWNER", body.get("role"));
        assertEquals(true, body.get("canEdit"));
        assertEquals(true, body.get("canDelete"));
        assertEquals(true, body.get("isOwner"));
        assertEquals(false, body.get("isModerator"));
    }
}