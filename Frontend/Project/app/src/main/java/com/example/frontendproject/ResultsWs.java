package com.example.frontendproject;

import com.google.gson.Gson;
import java.util.function.Consumer;
import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class ResultsWs {
    private final Gson gson = new Gson();
    private StompClient client;
    private Disposable resultsSub;
    // For emulator: ws://10.0.2.2:8080/ws/live-results/websocket
    private final String wsUrl;

    public ResultsWs(String wsUrl) { this.wsUrl = wsUrl; }

    public void connect() {
        client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
        // Leyla said heartbeats are off on server; keep client HB off as well
        // client.withClientHeartbeat(10000).withServerHeartbeat(10000);
        client.connect(); // no auth header
    }

    public boolean isConnected() { return client != null && client.isConnected(); }

    public void disconnect() {
        if (resultsSub != null) resultsSub.dispose();
        if (client != null) client.disconnect();
    }

    public void subscribeResults(long pollId,
                                 Consumer<ResultUpdate> onEvent,
                                 Consumer<String> onError) {
        String topic = "/topic/polls/" + pollId + "/results";
        resultsSub = client.topic(topic).subscribe(frame -> {
            ResultUpdate ru = gson.fromJson(frame.getPayload(), ResultUpdate.class);
            onEvent.accept(ru);
        }, t -> onError.accept(t.getMessage()));
    }

    public void sendVote(long pollId, VoteRequest vote) {
        String dest = "/app/polls/" + pollId + "/vote";
        client.send(dest, gson.toJson(vote)).subscribe();
    }
}
