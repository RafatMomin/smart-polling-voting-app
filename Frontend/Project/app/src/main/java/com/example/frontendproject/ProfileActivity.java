package com.example.frontendproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

public class ProfileActivity extends AppCompatActivity {
    private SessionManager session;
    private EditText etName;
    private TextView tvWelcome;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_profile);
        findViewById(R.id.btnChatbot).setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, ChatActivity.class))
        );


        session = new SessionManager(this);
        etName = findViewById(R.id.etName);
        tvWelcome = findViewById(R.id.tvWelcome);

        // Show user info from session (fallback)
        String userId = (session.getUserId() == null ? "" : session.getUserId());
        tvWelcome.setText("Welcome, user " + userId);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete());
        findViewById(R.id.btnLogout).setOnClickListener(v -> logout());

        // Open Poll & Vote → first go to MapActivity for location gate
        Button btnOpen = findViewById(R.id.btnOpen);
        btnOpen.setText("Open Poll & Vote");
        btnOpen.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, MapActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        verifyCurrentUser(); // GET /users/me on each return to this screen
    }

    private void verifyCurrentUser() {
        final String token = session.getToken();
        if (token == null || token.trim().isEmpty()) {
            // no token -> treat as logged out
            session.logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        final String url = "http://coms-3090-036.class.las.iastate.edu:8080/users/me";

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                resp -> {
                    // API doc: { "user": "user@example.com" }
                    String who = resp.optString("user", "");
                    if (who == null || who.trim().isEmpty()) {
                        String fallback = session.getUserId();
                        who = (fallback == null ? "" : fallback);
                    }
                    tvWelcome.setText("Welcome, " + who);
                },
                error -> {
                    int code = -1;
                    String body = "";
                    if (error != null && error.networkResponse != null) {
                        code = error.networkResponse.statusCode;
                        if (error.networkResponse.data != null) {
                            body = new String(error.networkResponse.data);
                        }
                    }
                    // Per API: { "message": "Invalid or expired token" }
                    if (code == 401 || code == 403) {
                        Toast.makeText(ProfileActivity.this,
                                "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
                        session.logout();
                        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        // keep user on screen; show info
                        Toast.makeText(ProfileActivity.this,
                                "Failed to verify user (" + code + "): " + body, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("Accept", "application/json");
                // API doc uses raw token in Authorization header (not "Bearer ...")
                h.put("Authorization", token);
                return h;
            }
        };

        AppRequestQueue.get(this).add(req);
    }

    private void save() {
        // Mock save → just toast and update welcome text
        String newName = etName.getText().toString().trim();
        tvWelcome.setText("Welcome, " + newName);
        Toast.makeText(this, "✅ Changes saved locally", Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete account?")
                .setMessage("This will log you out.")
                .setPositiveButton("Delete", (d, w) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        // Mock delete → clear session and return to login
        session.logout();
        Toast.makeText(this, "🗑️ Account deleted", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void logout() {
        session.logout();
        Toast.makeText(this, "🚪 Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}

