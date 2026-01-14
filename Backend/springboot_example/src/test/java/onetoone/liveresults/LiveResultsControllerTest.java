package onetoone.liveresults;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import onetoone.LiveResults.controller.LiveResultsController;
import onetoone.LiveResults.dto.LiveResultsMessage;
import onetoone.LiveResults.service.LiveResultsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LiveResultsControllerTest {

    @Mock
    private LiveResultsService liveResultsService;

    @InjectMocks
    private LiveResultsController controller;

    /*
     * LIVE RESULTS FEATURE TEST: initial subscription returns snapshot.
     * verifies sendInitialResults delegates to LiveResultsService.
     */
    @Test
    public void sendInitialResults_delegatesToService() {
        LiveResultsMessage msg = new LiveResultsMessage();
        when(liveResultsService.getResultsSnapshot(1L)).thenReturn(msg);

        LiveResultsMessage result = controller.sendInitialResults(1L);
        assertSame(msg, result);
    }

    /*
     * LIVE RESULTS FEATURE TEST: REST GET /live-results/poll/{id}.
     * should use getResultsSnapshot as well.
     */
    @Test
    public void getPollResults_delegatesToService() {
        LiveResultsMessage msg = new LiveResultsMessage();
        when(liveResultsService.getResultsSnapshot(2L)).thenReturn(msg);

        LiveResultsMessage result = controller.getPollResults(2L);
        assertSame(msg, result);
    }

    /*
     * LIVE RESULTS FEATURE TEST: WebSocket refresh mapping.
     * refreshPollResults should also call getResultsSnapshot.
     */
    @Test
    public void refreshPollResults_delegatesToService() {
        LiveResultsMessage msg = new LiveResultsMessage();
        when(liveResultsService.getResultsSnapshot(3L)).thenReturn(msg);

        LiveResultsMessage result = controller.refreshPollResults(3L);
        assertSame(msg, result);
    }
}