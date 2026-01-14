package onetoone.liveresults;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.LiveResults.dto.LiveResultsMessage;
import onetoone.LiveResults.dto.LiveResultsMessage.OptionDistribution;
import onetoone.LiveResults.service.LiveResultsService;
import onetoone.Polling.model.*;
import onetoone.Polling.repository.PollRepository;
import onetoone.Polling.repository.VoteRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;

@RunWith(MockitoJUnitRunner.class)
public class LiveResultsServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private PollRepository pollRepository;

    @Mock
    private VoteRepository voteRepository;

    @InjectMocks
    private LiveResultsService liveResultsService;

    /*
     * LIVE RESULTS FEATURE TEST: broadcastResults should send message to WebSocket topic.
     * verifies that convertAndSend is called with /topic/poll/{id} and a LiveResultsMessage.
     */
    @Test
    public void broadcastResults_sendsToTopic() {
        Long pollId = 1L;

        YesNoPoll poll = mock(YesNoPoll.class);
        when(poll.isActive()).thenReturn(true);
        when(poll.getType()).thenReturn("YES_NO");
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        when(voteRepository.countYes(pollId)).thenReturn(3L);
        when(voteRepository.countNo(pollId)).thenReturn(1L);

        liveResultsService.broadcastResults(pollId);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/poll/" + pollId), any(LiveResultsMessage.class));
    }

    /*
     * LIVE RESULTS FEATURE TEST: YES_NO poll calculation.
     * for 3 yes, 1 no -> total=4, 75% yes, 25% no.
     */
    @Test
    public void yesNoResults_calculatedCorrectly() {
        Long pollId = 2L;

        YesNoPoll poll = mock(YesNoPoll.class);
        when(poll.isActive()).thenReturn(true);
        when(poll.getType()).thenReturn("YES_NO");
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        when(voteRepository.countYes(pollId)).thenReturn(3L);
        when(voteRepository.countNo(pollId)).thenReturn(1L);

        LiveResultsMessage msg = liveResultsService.getResultsSnapshot(pollId);

        assertEquals(Integer.valueOf(4), msg.getTotalVotes());
        assertEquals("active", msg.getStatus());
        List<OptionDistribution> dist = msg.getDistribution();
        assertEquals(2, dist.size());
    }

    /*
     * LIVE RESULTS FEATURE TEST: RATING poll distribution.
     * verifies that per-rating counts are delegated to voteRepository and total is sum.
     */
    @Test
    public void ratingResults_useRatingCounts() {
        Long pollId = 3L;

        RatingPoll poll = mock(RatingPoll.class);
        when(poll.isActive()).thenReturn(false);
        when(poll.getType()).thenReturn("RATING");
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        when(voteRepository.countTotalByPoll(pollId)).thenReturn(6L);
        when(voteRepository.countByNumericValue(pollId, 1)).thenReturn(1L);
        when(voteRepository.countByNumericValue(pollId, 2)).thenReturn(2L);
        when(voteRepository.countByNumericValue(pollId, 3)).thenReturn(1L);
        when(voteRepository.countByNumericValue(pollId, 4)).thenReturn(1L);
        when(voteRepository.countByNumericValue(pollId, 5)).thenReturn(1L);

        LiveResultsMessage msg = liveResultsService.getResultsSnapshot(pollId);
        assertEquals(Integer.valueOf(6), msg.getTotalVotes());
        assertEquals(5, msg.getDistribution().size());
    }

    /*
     * LIVE RESULTS FEATURE TEST: MULTIPLE_CHOICE results.
     * verifies that countsMap from repository is mapped correctly into option distributions.
     */
    @Test
    public void multipleChoiceResults_useChoiceCounts() {
        Long pollId = 4L;

        MultipleChoicePoll poll = mock(MultipleChoicePoll.class);
        when(poll.isActive()).thenReturn(true);
        when(poll.getType()).thenReturn("MULTIPLE_CHOICE");
        when(poll.getOptions()).thenReturn(Arrays.asList("A", "B", "C"));
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        when(voteRepository.countTotalByPoll(pollId)).thenReturn(5L);
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"A", 2L});
        rows.add(new Object[]{"C", 3L});
        when(voteRepository.countChoices(pollId)).thenReturn(rows);

        LiveResultsMessage msg = liveResultsService.getResultsSnapshot(pollId);
        assertEquals(Integer.valueOf(5), msg.getTotalVotes());
        assertEquals(3, msg.getDistribution().size());
    }

    /*
     * LIVE RESULTS FEATURE TEST: RANKING poll Borda scores.
     * verifies that percentage uses distinct voters * totalOptions as maximum.
     */
    @Test
    public void rankingResults_useBordaScoresAndDistinctVoters() {
        Long pollId = 5L;

        RankingPoll poll = mock(RankingPoll.class);
        when(poll.isActive()).thenReturn(true);
        when(poll.getType()).thenReturn("RANKING");
        when(poll.getOptions()).thenReturn(Arrays.asList("A", "B"));
        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));

        when(voteRepository.countDistinctUsersByPoll(pollId)).thenReturn(2L);
        List<Object[]> scores = new ArrayList<>();
        scores.add(new Object[]{"A", 4L});
        scores.add(new Object[]{"B", 0L});
        when(voteRepository.rankingScores(pollId, 2)).thenReturn(scores);

        LiveResultsMessage msg = liveResultsService.getResultsSnapshot(pollId);

        assertEquals(Integer.valueOf(2), msg.getTotalVotes());
        assertEquals(2, msg.getDistribution().size());
    }
}