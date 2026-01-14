package com.example.frontendproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LiveDiscussionActivity extends AppCompatActivity implements WebSocketListener {

    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private EditText input;

    private int discussionId = -1;
    private String token = "";
    private String wsUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_discussion);
        discussionId = getIntent().getIntExtra("discussionId", -1);
        token = getIntent().getStringExtra("token");
        wsUrl = getIntent().getStringExtra("endpoint");

        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scrollViewChat);
        input = findViewById(R.id.messageInput);
        ImageButton sendBtn = findViewById(R.id.sendButton);
        WebSocketManager.getInstance().setListener(this);
        WebSocketManager.getInstance().connect(wsUrl);

        sendBtn.setOnClickListener(v -> sendBroadcast());
    }

    /** Send a public/broadcast message per API */
    private void sendBroadcast() {
        String msg = input.getText().toString().trim();
        if (msg.isEmpty()) return;

        try {
            JSONObject o = new JSONObject();
            o.put("type", "BROADCAST");
            o.put("content", msg);
            WebSocketManager.getInstance().send(o.toString());
            input.setText("");
        } catch (Exception ignored) { }
    }

    /** Optional: sample direct-message helper (not wired to UI) */
    private void sendDirect(String recipientEmail, String msg) {
        try {
            JSONObject o = new JSONObject();
            o.put("type", "DIRECT_MESSAGE");
            o.put("recipientEmail", recipientEmail);
            o.put("content", msg);
            WebSocketManager.getInstance().send(o.toString());
        } catch (Exception ignored) { }
    }
    private void leaveDiscussion() {
        if (discussionId == -1 || token == null) return;

        String url = "http://coms-3090-036.class.las.iastate.edu:8080/discussions/" + discussionId + "/leave";

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                resp -> {  },
                err -> { }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Authorization", token);
                return h;
            }
        };
        AppRequestQueue.get(this).add(req);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        leaveDiscussion();
        WebSocketManager.getInstance().disconnect();
    }

    @Override
    public void onWebSocketOpen(ServerHandshake handshake) {
        addLine("Connected");
    }

    @Override
    public void onWebSocketMessage(String message) {
        runOnUiThread(() -> {
            try {
                JSONObject o = new JSONObject(message);
                String type = o.optString("type", "");

                switch (type) {
                    case "WELCOME": {
                        String m = o.optString("message", "Connected");
                        addLine("✅ " + m);
                        break;
                    }
                    case "USER_JOINED": {
                        JSONObject user = o.optJSONObject("user");
                        String who = (user != null) ? user.optString("name",
                                user.optString("emailId", "Someone")) : "Someone";
                        int pc = o.optInt("participantCount", -1);
                        addLine("👋 " + who + " joined" + (pc >= 0 ? " • participants: " + pc : ""));
                        break;
                    }
                    case "MESSAGE": {
                        JSONObject user = o.optJSONObject("user");
                        String who = (user != null) ? user.optString("name",
                                user.optString("emailId", "Unknown")) : "Unknown";
                        String content = o.optString("content", "");
                        addLine(who + ": " + content);
                        break;
                    }
                    case "DIRECT_MESSAGE": {
                        JSONObject sender = o.optJSONObject("sender");
                        String who = (sender != null) ? sender.optString("name",
                                sender.optString("emailId", "Unknown")) : "Unknown";
                        String content = o.optString("content", "");
                        addLine("📩 [DM] " + who + ": " + content);
                        break;
                    }
                    case "USER_LEFT": {
                        JSONObject user = o.optJSONObject("user");
                        String who = (user != null) ? user.optString("name",
                                user.optString("emailId", "Someone")) : "Someone";
                        int pc = o.optInt("participantCount", -1);
                        addLine("👋 " + who + " left" + (pc >= 0 ? " • participants: " + pc : ""));
                        break;
                    }
                    case "ERROR": {
                        addLine("⚠️ Error: " + o.optString("message", "Unknown"));
                        break;
                    }
                    default: {
                        addLine(message);
                    }
                }
            } catch (Exception e) {
                addLine(" Parse error: " + e.getMessage());
            }
        });
    }

    @Override
    public void onWebSocketClose(int code, String reason, boolean remote) {
        addLine("Disconnected");
    }

    @Override
    public void onWebSocketError(Exception ex) {
        addLine("! " + (ex != null ? ex.getMessage() : "Unknown websocket error"));
    }

    private void addLine(String text) {
        runOnUiThread(() -> {
            TextView tv = new TextView(this);
            tv.setText(text);
            chatContainer.addView(tv);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

}

