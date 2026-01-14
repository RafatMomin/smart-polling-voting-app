package com.example.frontendproject;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.frontendproject.net.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * ChatActivity provides a chatbot UI using RecyclerView.
 * It allows the user to send messages, shows them immediately in the chat,
 * then calls the backend chatbot API (authenticated if logged in, otherwise public)
 * and displays the chatbot response.
 */

    public class ChatActivity extends AppCompatActivity {

    private RecyclerView rv;
    private ChatAdapter adapter;
    private EditText et;
    private Button btn;
    /**
     * Initializes the chatbot screen UI, sets up RecyclerView + adapter,
     * and registers the Send button click listener.
     *
     * @param savedInstanceState previous saved instance state if any
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rv = findViewById(R.id.rvChat);
        et = findViewById(R.id.etMessage);
        btn = findViewById(R.id.btnSend);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);

        adapter = new ChatAdapter();
        rv.setAdapter(adapter);

        btn.setOnClickListener(v -> sendMessage());
        loadChatHistoryIfLoggedIn();
    }

    /**
     * Reads user message input, displays it in the chat UI,
     * sends it to the appropriate backend chatbot endpoint,
     * and updates the UI with the bot response.
     *
     * Uses authenticated endpoint if user is logged in,
     * otherwise uses public endpoint.
     */
    private void sendMessage() {
        String userText = et.getText().toString().trim();
        if (TextUtils.isEmpty(userText)) return;

        // UI: show user bubble immediately
        adapter.addUserMessage(userText);
        scrollToBottom();
        et.setText("");
        hideKeyboard();

        // Disable button & show typing indicator
        btn.setEnabled(false);
        adapter.addTypingIndicator();
        scrollToBottom();

        // Decide endpoint based on auth
        SessionManager session = new SessionManager(this);
        boolean loggedIn = session.isLoggedIn();

        // TODO: get your token from SessionManager. Adjust method name if different.
        // Many apps store it like "Bearer <jwt>" already. If you only have raw JWT,
        // prepend "Bearer " here.
        String token = null;
        if (loggedIn) {
            // Example guesses—rename to what your SessionManager exposes:
            // token = session.getToken();
            // token = session.getAuthToken();
            // token = "Bearer " + session.getJwt();
            token = session.getToken(); // <--- CHANGE THIS if your method name differs
            if (token != null && !token.toLowerCase().startsWith("bearer ")) {
                token = "Bearer " + token;
            }
        }

        // Make the call
        if (loggedIn && token != null) {
            // Authenticated chat
            com.example.frontendproject.net.RetrofitClient.chat()
                    .getChatAuth(token, userText)
                    .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                        @Override public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call,
                                                         retrofit2.Response<okhttp3.ResponseBody> response) {
                            btn.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null) {
                                try {
                                    String bot = response.body().string();
                                    if (TextUtils.isEmpty(bot)) bot = "(empty reply)";
                                    adapter.updateLastBotMessage(bot);
                                } catch (Exception e) {
                                    adapter.updateLastBotMessage("Error reading reply.");
                                }
                            } else {
                                // If backend returns JSON error on auth endpoints, show it as text
                                adapter.updateLastBotMessage("Server error: " + response.code());
                            }
                            scrollToBottom();
                        }

                        @Override public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                            btn.setEnabled(true);
                            adapter.updateLastBotMessage("Network error: " + t.getMessage());
                            scrollToBottom();
                        }
                    });

        } else {
            // Public chat (no auth)
            com.example.frontendproject.net.RetrofitClient.chat()
                    .getChatPublic(userText)
                    .enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                        @Override public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call,
                                                         retrofit2.Response<okhttp3.ResponseBody> response) {
                            btn.setEnabled(true);
                            if (response.isSuccessful() && response.body() != null) {
                                try {
                                    String bot = response.body().string();
                                    if (TextUtils.isEmpty(bot)) bot = "(empty reply)";
                                    adapter.updateLastBotMessage(bot);
                                } catch (Exception e) {
                                    adapter.updateLastBotMessage("Error reading reply.");
                                }
                            } else {
                                adapter.updateLastBotMessage("Server error: " + response.code());
                            }
                            scrollToBottom();
                        }

                        @Override public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                            btn.setEnabled(true);
                            adapter.updateLastBotMessage("Network error: " + t.getMessage());
                            scrollToBottom();
                        }
                    });
        }
    }

    /**
     * Scrolls the RecyclerView to the most recent chat message.
     */
    private void scrollToBottom() {
        rv.post(() -> rv.scrollToPosition(adapter.getItemCount() - 1));
    }
    /**
     * Hides the soft keyboard after the user sends a message.
     */
    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void loadChatHistoryIfLoggedIn() {
        SessionManager session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            return;
        }

        String token = session.getToken();

        if (token == null || token.isEmpty()) return;

        if (!token.toLowerCase().startsWith("bearer ")) {
            token = "Bearer " + token;
        }

        RetrofitClient.chat().getHistory(token).enqueue(new Callback<List<com.example.frontendproject.net.ChatHistoryItem>>() {
            @Override
            public void onResponse(Call<List<com.example.frontendproject.net.ChatHistoryItem>> call,
                                   Response<List<com.example.frontendproject.net.ChatHistoryItem>> response) {

                if (!response.isSuccessful() || response.body() == null) return;

                List<com.example.frontendproject.net.ChatHistoryItem> history = response.body();
                List<ChatMessage> preload = new ArrayList<>();

                // Convert backend history → UI messages
                for (com.example.frontendproject.net.ChatHistoryItem h : history) {
                    preload.add(new ChatMessage(ChatMessage.ROLE_USER, h.userMessage));
                    preload.add(new ChatMessage(ChatMessage.ROLE_BOT, h.botResponse));
                }

                adapter.setMessages(preload);
                scrollToBottom();
            }

            @Override
            public void onFailure(Call<List<com.example.frontendproject.net.ChatHistoryItem>> call, Throwable t) {

            }
        });
    }

}
