package onetoone.LiveResults.controller;

import onetoone.LiveResults.dto.LiveResultsMessage;
import onetoone.LiveResults.service.LiveResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/live-results")
@CrossOrigin(origins = "*") // for testing
public class LiveResultsController {

    @Autowired
    private LiveResultsService liveResultsService;

    // when someone first subscribes, send results to them
    @SubscribeMapping("/poll/{pollId}")
    public LiveResultsMessage sendInitialResults(@DestinationVariable Long pollId) {
        return liveResultsService.getResultsSnapshot(pollId);
    }

    /* get current poll results
     * optional for testing and to get initial data before subscribing to websocket
     * Frontend can call this to get initial data before subscribing to WebSocket
     */
    @GetMapping("/poll/{pollId}")
    public LiveResultsMessage getPollResults(@PathVariable Long pollId) {
        return liveResultsService.getResultsSnapshot(pollId);
    }

    /* WebSocket message handler: allows a request to refresh by using WebSocket
     * to request send to: /app/poll/{pollId}/refresh
     * response broadcasts to: /topic/poll/{pollId}
     * optional manual refresh for testing
     */
    @MessageMapping("/poll/{pollId}/refresh")
    @SendTo("/topic/poll/{pollId}")
    public LiveResultsMessage refreshPollResults(@DestinationVariable Long pollId) {
        return liveResultsService.getResultsSnapshot(pollId);
    }
}