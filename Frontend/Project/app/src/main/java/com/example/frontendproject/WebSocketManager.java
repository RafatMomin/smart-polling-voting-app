package com.example.frontendproject;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebSocketManager {

    private static WebSocketManager instance;
    private WebSocketClient client;
    private WebSocketListener listener;

    public static WebSocketManager getInstance() {
        if (instance == null) instance = new WebSocketManager();
        return instance;
    }

    public void setListener(WebSocketListener l) {
        this.listener = l;
    }

    public synchronized void connect(String url) {
        disconnect();

        Log.d("WEBSOCKET", "Connecting: " + url);

        client = new WebSocketClient(URI.create(url), new Draft_6455()) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                if (listener != null) listener.onWebSocketOpen(handshakedata);
            }

            @Override
            public void onMessage(String message) {
                if (listener != null) listener.onWebSocketMessage(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (listener != null) listener.onWebSocketClose(code, reason, remote);
            }

            @Override
            public void onError(Exception ex) {
                if (listener != null) listener.onWebSocketError(ex);
            }
        };

        client.connect();
    }

    public synchronized void send(String text) {
        if (client != null && client.isOpen()) {
            client.send(text);
        } else {
            Log.w("WEBSOCKET", "send() called while socket not open");
        }
    }

    public synchronized void disconnect() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (Exception ignored) { }
    }
}

