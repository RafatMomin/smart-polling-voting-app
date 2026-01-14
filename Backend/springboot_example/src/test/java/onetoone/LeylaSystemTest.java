package onetoone;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import onetoone.Dashboard.service.DashboardService;
import onetoone.LiveResults.service.LiveResultsService;
import onetoone.Location.service.LocationService;
import onetoone.Polling.model.Poll;
import onetoone.Polling.model.RankingPoll;
import onetoone.Polling.model.YesNoPoll;
import onetoone.Polling.model.Vote;
import onetoone.Polling.service.PollService;
import onetoone.Users.model.Users;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class LeylaSystemTest {

    @LocalServerPort
    int port;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private PollService pollService;

    @MockBean
    private LocationService locationService;

    @MockBean
    private LiveResultsService liveResultsService;

    @Before
    public void setUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
    }

    /*
     * TEST FOR DASHBOARD FEATURE: returns how many polls a user has joined
     * when a valid token is given the dashboard should return how many polls the user has joined
     * mock the service to pretend the user joined 7 polls
     */
    @Test
    public void dashboard_returnsPollsJoined() {
        String token = "123456";
        Users user = new Users();
        user.setEmailId("leylak@iastate.edu");

        when(dashboardService.getUserFromToken(token)).thenReturn(user);
        when(dashboardService.getTotalPollsJoined("leylak@iastate.edu")).thenReturn(7L);

        Response response = RestAssured.given()
                .header("Authorization", token)
                .when()
                .get("/dashboard/polls-joined");

        assertEquals(200, response.getStatusCode());

        int count = response.jsonPath().getInt("count_polls_joined");
        assertEquals(7, count);
    }

    /*
     * PRIV/PUBLIC ACCESS FEATURE TEST: if private polls reject users without permission and allows members with permission (owners and members)
     * if not a member you get an error
     * if you are a member you get the polls results
     */
    @Test
    public void getResults_privatePoll_nonMemberDenied_memberAllowed() {
        Long pollId = 10L;
        RankingPoll privatePoll = new RankingPoll("Which letter is the best?", List.of("A", "B", "C"));
        privatePoll.setVisibility(Poll.Visibility.PRIVATE);
        privatePoll.setCreatedAt(LocalDateTime.now());
        when(pollService.findPoll(pollId)).thenReturn(Optional.of(privatePoll));

        // case 1: user is not a member and should be denied
        String stranger = "notamember@email.com";
        when(pollService.isOwner(pollId, stranger)).thenReturn(false);
        when(pollService.isMember(pollId, stranger)).thenReturn(false);

        Response respStranger = RestAssured.given()
                .param("emailId", stranger)
                .when()
                .get("/polls/{pollId}/results", pollId);

        assertEquals(200, respStranger.getStatusCode());
        String errorMessage = respStranger.jsonPath().getString("error");
        assertEquals("Access denied, the user is not a member of this poll.", errorMessage);

        // case 2: user is a member/owner so should get results
        String member = "member@email.com";
        when(pollService.isOwner(pollId, member)).thenReturn(true);
        when(pollService.isMember(pollId, member)).thenReturn(true);
        when(pollService.getRankingStats(pollId, 3)).thenReturn(Map.of("best", "A"));

        Response respMember = RestAssured.given()
                .param("emailId", member)
                .when()
                .get("/polls/{pollId}/results", pollId);

        assertEquals(200, respMember.getStatusCode());
        String best = respMember.jsonPath().getString("best");
        assertEquals("A", best);
    }

    /*
     * LOCATION VERIFICATION FEATURE TEST: invalid location prevents voting and results broadcast.
     * if the user is outside the allowed campus area locationService should throw an error
     * when location is invalid the vote should not be added and live results should not be updated
     */
    @Test
    public void invalidLocation_returnsError_andDoesNotBroadcastOrVote() {
        Long pollId = 20L;
        YesNoPoll poll = new YesNoPoll("Is ISU the best university ever?");
        poll.setVisibility(Poll.Visibility.PUBLIC);
        when(pollService.findPoll(pollId)).thenReturn(Optional.of(poll));

        // make user be outside geofence
        doThrow(new RuntimeException("Location outside allowed area"))
                .when(locationService).validateLocation(anyDouble(), anyDouble());

        Map<String, Object> voteRequest = Map.of("lat", 41.0, "lng", -93.0);

        Response response = RestAssured.given()
                .queryParam("userId", "voter@example.com")
                .queryParam("numericValue", "1")
                .contentType(ContentType.JSON)
                .body(voteRequest)
                .when()
                .post("/polls/{id}/vote", pollId);

        assertEquals(200, response.getStatusCode());
        String error = response.jsonPath().getString("error");
        assertEquals("Location outside allowed area", error);

        // verify no vote is saved and live results are not broadcast
        verify(pollService, never()).addVote(anyLong(), any(Vote.class));
        verify(liveResultsService, never()).broadcastResults(anyLong());
    }

    /*
     * LOCATION VERIFICATION FEATURE TEST: valid location allows voting and results broadcast.
     * if the user is inside the geofence locationService should not throw an error
     * when location is valid the vote should be added and live results should be updated
     */
    @Test
    public void validLocation_broadcastsLiveResults_castsVote() {
        Long pollId = 21L;
        YesNoPoll poll = new YesNoPoll("Is winter the best season?");
        poll.setVisibility(Poll.Visibility.PUBLIC);
        when(pollService.findPoll(pollId)).thenReturn(Optional.of(poll));

        doNothing().when(locationService).validateLocation(anyDouble(), anyDouble());
        doNothing().when(pollService).addVote(eq(pollId), any(Vote.class));

        Map<String, Object> voteRequest = Map.of("lat", 41.5, "lng", -93.5);

        Response response = RestAssured.given()
                .queryParam("userId", "voter@example.com")
                .queryParam("numericValue", "0")
                .contentType(ContentType.JSON)
                .body(voteRequest)
                .when()
                .post("/polls/{id}/vote", pollId);

        assertEquals(200, response.getStatusCode());
        String message = response.jsonPath().getString("message");
        assertEquals("Vote added", message);

        // check that location validation and live results were called
        verify(locationService, times(1)).validateLocation(anyDouble(), anyDouble());
        verify(liveResultsService, times(1)).broadcastResults(pollId);
    }
}
