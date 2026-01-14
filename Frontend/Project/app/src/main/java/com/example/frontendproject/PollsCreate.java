package com.example.frontendproject;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Merged PollsCreate:
 * - Type spinner (YES OR NO, RATING, MULTIPLE_CHOICE, RANKING)
 * - Visibility spinner (PUBLIC/PRIVATE) – optional (layout may or may not have it)
 * - Owner email capture (prompt-once if missing)
 * - Options parsing for MCQ/Ranking
 * - Private poll: access code dialog with Copy
 * - Admin/Moderator chip entry (optional)
 * - Creates Discussion when token is present, but ALWAYS opens PollingWindow on success
 */
public class PollsCreate extends AppCompatActivity {

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080";

    // Session / token
    private SessionManager session;
    private String token;

    // Common UI
    private TextInputEditText etTitle, etOptions, etContext;
    private TextInputLayout tilOptions;
    private Spinner spType;
    private Spinner spVisibility;   // may be null if not in layout
    private Button btnCreate;
    private ProgressBar progress;

    // Optional admin/moderator UI (present only in some layouts)
    private TextInputEditText etAdminEmail, etModeratorEmail;
    private Button btnAddAdmin, btnAddModerator;
    private ChipGroup chipAdmins, chipModerators;

    // Data for chips
    private final ArrayList<String> adminEmails = new ArrayList<>();
    private final ArrayList<String> moderatorEmails = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_create);

        session = new SessionManager(this);
        token   = session.getToken();

        // Bind common views
        etTitle      = findViewById(R.id.etTitle);
        etOptions    = findViewById(R.id.etOptions);
        tilOptions   = findViewById(R.id.tilOptions);
        spType       = findViewById(R.id.spType);
        spVisibility = findViewById(R.id.spVisibility); // optional
        btnCreate    = findViewById(R.id.btnCreate);
        progress     = findViewById(R.id.progress);
        etContext    = findViewById(R.id.etContext);

        // Optional admin/moderator views (may be absent)
        etAdminEmail     = findViewById(R.id.etAdminEmail);
        etModeratorEmail = findViewById(R.id.etModeratorEmail);
        btnAddAdmin      = findViewById(R.id.btnAddAdmin);
        btnAddModerator  = findViewById(R.id.btnAddModerator);
        chipAdmins       = findViewById(R.id.chipAdmins);
        chipModerators   = findViewById(R.id.chipModerators);

        if (btnAddAdmin != null && etAdminEmail != null && chipAdmins != null) {
            btnAddAdmin.setOnClickListener(v -> addChip(etAdminEmail, chipAdmins, adminEmails));
        }
        if (btnAddModerator != null && etModeratorEmail != null && chipModerators != null) {
            btnAddModerator.setOnClickListener(v -> addChip(etModeratorEmail, chipModerators, moderatorEmails));
        }

        // Type spinner
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"YES OR NO", "RATING", "MULTIPLE_CHOICE", "RANKING"}
        );
        spType.setAdapter(a);

        spType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String t = (spType.getSelectedItem() != null) ? spType.getSelectedItem().toString() : "";
                boolean needsOptions = t.equals("MULTIPLE_CHOICE") || t.equals("RANKING");
                if (tilOptions != null) tilOptions.setVisibility(needsOptions ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // Visibility spinner (PUBLIC/PRIVATE) — only if present in layout
        if (spVisibility != null) {
            ArrayAdapter<String> visAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"PUBLIC", "PRIVATE"}
            );
            spVisibility.setAdapter(visAdapter);
        }

        btnCreate.setOnClickListener(v -> createPoll());
    }

    // --------------------------------
    // Admin/Moderator chip helpers
    // --------------------------------
    private void addChip(TextInputEditText input, ChipGroup group, ArrayList<String> list) {
        String email = (input.getText() != null) ? input.getText().toString().trim() : "";
        if (email.isEmpty()) return;
        if (list.contains(email)) return;

        list.add(email);

        Chip c = new Chip(this);
        c.setText(email);
        c.setCloseIconVisible(true);
        c.setOnCloseIconClickListener(v -> {
            group.removeView(c);
            list.remove(email);
        });

        group.addView(c);
        input.setText("");
    }

    // --------------------------------
    // Create poll (merged logic)
    // --------------------------------
    private void createPoll() {
        String title = (etTitle != null && etTitle.getText() != null) ? etTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = (spType.getSelectedItem() != null) ? spType.getSelectedItem().toString() : "";
        if (type.isEmpty()) {
            Toast.makeText(this, "Please select a type", Toast.LENGTH_SHORT).show();
            return;
        }

        // Visibility defaults to PUBLIC if spinner not present
        String visibility = "PUBLIC";
        if (spVisibility != null && spVisibility.getSelectedItem() != null) {
            visibility = spVisibility.getSelectedItem().toString();
        }
        final String visibilityFinal = visibility;

        // Endpoint path
        String path;
        switch (type) {
            case "YES OR NO":       path = "/polls/yesno";    break;
            case "RATING":          path = "/polls/rating";   break;
            case "MULTIPLE_CHOICE": path = "/polls/multiple"; break;
            case "RANKING":         path = "/polls/ranking";  break;
            default:
                Toast.makeText(this, "Unknown type", Toast.LENGTH_SHORT).show();
                return;
        }

        // Build body
        JSONObject body = new JSONObject();
        try {
            body.put("title", title);
            body.put("visibility", visibilityFinal);

            // Owner emailId (prompt-once if missing)
            String emailId = session.getEmail();
            if (emailId == null || emailId.trim().isEmpty()) {
                String candidate = session.getUserId();
                if (candidate != null && candidate.contains("@")) {
                    emailId = candidate;
                    session.setEmail(emailId); // persist
                }
            }
            if (emailId == null || emailId.trim().isEmpty()) {
                promptForEmailAndTellUserToTapCreateAgain();
                return; // user will tap Create again after saving email
            }
            body.put("emailId", emailId);

            // context (optional)
            if (etContext != null && etContext.getText() != null) {
                String ctx = etContext.getText().toString().trim();
                if (!ctx.isEmpty()) body.put("context", ctx);
            }

            // Options (MCQ/Ranking)
            boolean needsOptions = type.equals("MULTIPLE_CHOICE") || type.equals("RANKING");
            if (needsOptions && etOptions != null && etOptions.getText() != null) {
                String raw = etOptions.getText().toString();
                String[] parts = raw.split(",");
                JSONArray arr = new JSONArray();
                for (String p : parts) {
                    if (p != null) {
                        String s = p.trim();
                        if (!s.isEmpty()) arr.put(s);
                    }
                }
                if (arr.length() == 0) {
                    Toast.makeText(this, "Enter at least one option (comma-separated)", Toast.LENGTH_SHORT).show();
                    return;
                }
                body.put("options", arr);
            }

            // Admin/Moderator arrays (if chip UI exists)
            if (emailId != null && !adminEmails.contains(emailId)) adminEmails.add(emailId);
            if (chipAdmins != null)     body.put("admins",     new JSONArray(adminEmails));
            if (chipModerators != null) body.put("moderators", new JSONArray(moderatorEmails));

        } catch (Exception ignore) { }

        setLoading(true);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                BASE + path,
                body,
                resp -> {
                    setLoading(false);

                    // Extract pollId from different response shapes
                    int pollId = -1;
                    try {
                        if (resp.has("id")) {
                            pollId = resp.optInt("id", -1);
                        }
                        if (pollId <= 0 && resp.has("poll")) {
                            JSONObject pollObj = resp.optJSONObject("poll");
                            if (pollObj != null) {
                                pollId = pollObj.optInt("id", -1);
                            }
                        }
                    } catch (Exception ignore) {}

                    // Show access code for PRIVATE if present
                    String accessCode = null;
                    if ("PRIVATE".equalsIgnoreCase(visibilityFinal)) {
                        if (resp.has("accessCode")) {
                            accessCode = resp.optString("accessCode", null);
                        }
                        if (accessCode == null && resp.has("poll")) {
                            JSONObject pollObj = resp.optJSONObject("poll");
                            if (pollObj != null && pollObj.has("accessCode")) {
                                accessCode = pollObj.optString("accessCode", null);
                            }
                        }
                    }

                    // What to do next:
                    final int finalPollId = pollId;
                    Runnable gotoNext = () -> {
                        // If we have a pollId, we ALWAYS open the PollingWindow.
                        if (finalPollId > 0) {
                            // If we have a token, try to create a discussion first; otherwise go straight to window.
                            if (token != null) {
                                createDiscussionForPoll(finalPollId, title);
                            } else {
                                openPollingWindow(finalPollId, title);
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "Poll created, but missing ID from server.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    };

                    if ("PRIVATE".equalsIgnoreCase(visibilityFinal) &&
                            accessCode != null && !accessCode.isEmpty()) {
                        final String codeToCopy = accessCode;
                        new AlertDialog.Builder(this)
                                .setTitle("Private Poll Created")
                                .setMessage("Share this access code with participants:\n\n" + accessCode)
                                .setPositiveButton("Copy", (d, w) -> {
                                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                                    if (cm != null) {
                                        cm.setPrimaryClip(ClipData.newPlainText("Access Code", codeToCopy));
                                        Toast.makeText(this, "Access code copied", Toast.LENGTH_SHORT).show();
                                    }
                                    gotoNext.run();
                                })
                                .setNegativeButton("Open Window", (d, w) -> gotoNext.run())
                                .show();
                    } else {
                        gotoNext.run();
                    }
                },
                err -> {
                    setLoading(false);
                    int code = -1;
                    String msg = "";
                    if (err != null && err.networkResponse != null) {
                        code = err.networkResponse.statusCode;
                        if (err.networkResponse.data != null) {
                            msg = new String(err.networkResponse.data);
                        }
                    }
                    Toast.makeText(this, "Create failed (" + code + "): " + msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                if (token != null) h.put("Authorization", token);
                h.put("Accept", "application/json");
                h.put("Content-Type", "application/json");
                return h;
            }
        };

        AppRequestQueue.get(this).add(req);
    }

    // Create a discussion for the poll (if token present). On success OR failure, still open window.
    private void createDiscussionForPoll(int pollId, String title) {
        try {
            JSONObject body = new JSONObject();
            body.put("title", "Discussion: " + title);
            body.put("description", "Discussion space for poll: " + title);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    BASE + "/discussions/poll/" + pollId,
                    body,
                    resp -> {
                        Toast.makeText(this, "Poll + Discussion Created!", Toast.LENGTH_SHORT).show();
                        openPollingWindow(pollId, title);
                        finish();
                    },
                    err -> {
                        Toast.makeText(this, "Discussion creation failed; opening poll.", Toast.LENGTH_SHORT).show();
                        openPollingWindow(pollId, title);
                        finish();
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> h = new HashMap<>();
                    if (token != null) h.put("Authorization", token);
                    h.put("Content-Type", "application/json");
                    return h;
                }
            };

            AppRequestQueue.get(this).add(req);
        } catch (Exception ignored) {
            openPollingWindow(pollId, title);
            finish();
        }
    }

    // Navigate to PollingWindow with the new pollId
    private void openPollingWindow(int pollId, String title) {
        try {
            Intent i = new Intent(PollsCreate.this, PollingWindow.class);
            i.putExtra("pollId", pollId);
            i.putExtra("title", title);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open Polling Window", Toast.LENGTH_LONG).show();
        }
    }

    // Loading state
    private void setLoading(boolean on) {
        if (progress != null) progress.setVisibility(on ? View.VISIBLE : View.GONE);
        if (btnCreate != null) btnCreate.setEnabled(!on);
    }

    // Email prompt (one-time)
    private void promptForEmailAndTellUserToTapCreateAgain() {
        final TextInputEditText input = new TextInputEditText(this);
        input.setHint("you@iastate.edu");
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(this)
                .setTitle("Add your email")
                .setMessage("Enter your email once so private polls show up for you.")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String typed = (input.getText() != null) ? input.getText().toString().trim() : "";
                    if (typed.isEmpty() || !typed.contains("@")) {
                        Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
                    } else {
                        session.setEmail(typed);
                        Toast.makeText(this, "Saved! Tap Create again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
