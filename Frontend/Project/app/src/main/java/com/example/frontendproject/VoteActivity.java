package com.example.frontendproject;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Merged VoteActivity:
 * - Voting UI (YES_NO, RATING, MULTIPLE_CHOICE, RANKING)
 * - Results screen button
 * - Rotate Access Code (owner-only) + fetch code dialog
 * - Live Discussion FAB (discover discussion, join, open LiveDiscussionActivity)
 * - Polling Window / Access-Level gating (enables/disables voting for user)
 * - Admin detection to bypass window gate
 */
public class VoteActivity extends AppCompatActivity {

    // ---- Intent data ----
    private int pollId = -1;
    private String pollTitle = "";
    private String pollType = "";
    private ArrayList<String> pollOptions;

    // ---- Extras (optional) ----
    private double latExtra = Double.NaN;
    private double lngExtra = Double.NaN;

    // ---- UI refs ----
    private TextView txtTitle;
    private TextView chipType;
    private TextView chipStatus;
    private LinearLayout container;
    private Button btnSubmit;
    private Button btnResults;

    private RadioGroup rgYesNo;
    private RatingBar ratingBar;
    private RadioGroup rgChoices;

    private static class RankRow {
        String option;
        NumberPicker picker;
    }
    private final ArrayList<RankRow> rankRows = new ArrayList<>();

    private SessionManager session;
    private boolean isAdmin = false;

    // ---- Network constants ----
    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080";

    // ---- Menu constants ----
    private static final int MENU_ROTATE_CODE = 2001;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        session = new SessionManager(this);
        Log.d("SESSION_TOKEN_CHECK", "Token in session = " + session.getToken());

        // Read intent data
        pollId = getIntent().getIntExtra("pollId", -1);
        pollTitle = getIntent().getStringExtra("title");
        pollType = getIntent().getStringExtra("type");
        pollOptions = getIntent().getStringArrayListExtra("options");
        latExtra = getIntent().getDoubleExtra("lat", Double.NaN);
        lngExtra = getIntent().getDoubleExtra("lng", Double.NaN);

        boolean inflated = tryInflateXml();
        if (!inflated) {
            buildFallbackUi();
            return;
        }

        if (txtTitle != null) {
            txtTitle.setText(pollTitle != null ? pollTitle : "");
        }
        if (chipType != null) {
            chipType.setText(pollType != null ? pollType : "");
        }
        if (chipStatus != null) chipStatus.setText("Online");

        buildInputsForType();

        // Gate UI by polling window/access-level (non-admin users)
        checkWindowStatus();
        // Load admin status (admins always can vote)
        loadAdminStatus();

        // Actions
        btnSubmit.setOnClickListener(v -> doSubmit());
        btnResults.setOnClickListener(v -> openResults());

        // Live Discussion FAB (if present in layout)
        FloatingActionButton fabLiveDiscussion = findViewById(R.id.fabLiveDiscussion);
        if (fabLiveDiscussion != null) {
            fabLiveDiscussion.setOnClickListener(v -> {
                Log.d("FAB_CLICK", "Live Discussion FAB pressed");
                startDiscussion();
            });
        }
    }

    // ----------------------------
    // Admin & Polling Window logic
    // ----------------------------

    private void loadAdminStatus() {
        if (pollId <= 0) return;

        String url = BASE + "/polls/" + pollId;
        StringRequest req = new StringRequest(Request.Method.GET, url,
                resp -> {
                    try {
                        JSONObject obj = new JSONObject(resp);
                        JSONArray admins = obj.optJSONArray("admins");

                        String me = session.getEmail();
                        if (me == null || me.trim().isEmpty()) me = session.getUserId();

                        if (admins != null && me != null) {
                            for (int i = 0; i < admins.length(); i++) {
                                if (me.equalsIgnoreCase(admins.getString(i))) {
                                    isAdmin = true;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignore) {}
                    // Re-evaluate window with admin flag
                    checkWindowStatus();
                },
                err -> checkWindowStatus()
        );
        AppRequestQueue.get(this).add(req);
    }

    private void checkWindowStatus() {
        // Admins can always vote
        if (isAdmin) {
            enableVotingUi();
            return;
        }

        String user = session.getEmail();
        if (user == null || user.trim().isEmpty()) user = session.getUserId();
        if (pollId <= 0 || user == null || user.trim().isEmpty()) {
            disableVotingUi("Invalid poll/user context.");
            return;
        }

        String url = BASE + "/polling-windows/poll/" + pollId;

        String finalUser = user;
        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                arr -> {
                    try {
                        if (arr.length() == 0) {
                            disableVotingUi("This poll has no active voting window.");
                            return;
                        }

                        JSONObject winObj = arr.getJSONObject(0);
                        boolean withinTime = winObj.optBoolean("withinTimeWindow", false);

                        JSONObject windowData = winObj.optJSONObject("window");
                        if (windowData == null) {
                            disableVotingUi("Invalid poll window.");
                            return;
                        }

                        String creatorEmail = windowData.optString("creatorEmail", "");
                        boolean isCreator = finalUser.equalsIgnoreCase(creatorEmail);

                        boolean isAssigned = false;
                        JSONArray groups = windowData.optJSONArray("targetGroups");
                        if (groups != null) {
                            outer:
                            for (int g = 0; g < groups.length(); g++) {
                                JSONObject group = groups.optJSONObject(g);
                                if (group == null) continue;
                                JSONArray members = group.optJSONArray("memberEmails");
                                if (members != null) {
                                    for (int m = 0; m < members.length(); m++) {
                                        if (finalUser.equalsIgnoreCase(members.getString(m))) {
                                            isAssigned = true;
                                            break outer;
                                        }
                                    }
                                }
                            }
                        }

                        if (isCreator || (isAssigned && withinTime)) {
                            enableVotingUi();
                        } else {
                            disableVotingUi("You are not allowed to vote in this poll.");
                        }

                    } catch (Exception e) {
                        disableVotingUi("Error checking poll window.");
                    }
                },
                err -> disableVotingUi("Failed to validate access.")
        );
        AppRequestQueue.get(this).add(req);
    }

    private void disableVotingUi(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        if (btnSubmit != null) btnSubmit.setEnabled(false);
        if (rgYesNo != null) for (int i = 0; i < rgYesNo.getChildCount(); i++) rgYesNo.getChildAt(i).setEnabled(false);
        if (ratingBar != null) ratingBar.setIsIndicator(true);
        if (rgChoices != null) for (int i = 0; i < rgChoices.getChildCount(); i++) rgChoices.getChildAt(i).setEnabled(false);
        for (RankRow rr : rankRows) rr.picker.setEnabled(false);
    }

    private void enableVotingUi() {
        if (btnSubmit != null) btnSubmit.setEnabled(true);
        if (rgYesNo != null) for (int i = 0; i < rgYesNo.getChildCount(); i++) rgYesNo.getChildAt(i).setEnabled(true);
        if (ratingBar != null) ratingBar.setIsIndicator(false);
        if (rgChoices != null) for (int i = 0; i < rgChoices.getChildCount(); i++) rgChoices.getChildAt(i).setEnabled(true);
        for (RankRow rr : rankRows) rr.picker.setEnabled(true);
    }

    // ----------------------------
    // Live Discussion flow
    // ----------------------------

    private void startDiscussion() {
        Log.d("DISCUSSION_FLOW", "startDiscussion() called, token=" + session.getToken() + " pollId=" + pollId);

        String token = session.getToken();
        String url = BASE + "/discussions?pollId=" + pollId;

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    Log.d("DISCUSSION_FLOW", "GET success: " + resp);
                    try {
                        JSONArray arr = new JSONArray(resp);
                        if (arr.length() == 0) {
                            Toast.makeText(this, "No discussion exists for this poll.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONObject obj = arr.getJSONObject(0);
                        int discussionId = obj.getInt("id");
                        String wsEndpoint = "ws://coms-3090-036.class.las.iastate.edu:8080/discussion/"
                                + discussionId + "?token=" + token;

                        // Reset any active session (if your WebSocketManager supports it)
                        try { WebSocketManager.getInstance().disconnect(); } catch (Throwable ignore) {}

                        joinDiscussion(discussionId, wsEndpoint);

                    } catch (Exception ex) {
                        Log.e("DISCUSSION_FLOW", "JSON parse error: " + ex);
                    }
                },
                err -> {
                    Log.e("DISCUSSION_FLOW", "GET failed: " + err);
                    Toast.makeText(this, "Error loading discussion", Toast.LENGTH_SHORT).show();
                }
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

    private void joinDiscussion(int discussionId, String wsEndpoint) {
        String token = session.getToken();
        String url = BASE + "/discussions/" + discussionId + "/join";

        Log.d("JOIN_DEBUG", "Joining discussion: " + url + " token=" + token);

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                resp -> {
                    Log.d("JOIN_DEBUG", "Join success: " + resp);
                    openDiscussionScreen(discussionId, wsEndpoint);
                },
                err -> {
                    String errorBody = "";
                    if (err != null && err.networkResponse != null && err.networkResponse.data != null) {
                        errorBody = new String(err.networkResponse.data, StandardCharsets.UTF_8);
                    }

                    Log.e("JOIN_DEBUG", "Join FAILED: " + errorBody);
                    if (errorBody.contains("already") || errorBody.contains("Already")) {
                        openDiscussionScreen(discussionId, wsEndpoint);
                        return;
                    }

                    Toast.makeText(this, "JOIN FAILED: " + errorBody, Toast.LENGTH_LONG).show();
                }

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

    private void openDiscussionScreen(int discussionId, String wsEndpoint) {
        Intent i = new Intent(this, LiveDiscussionActivity.class);
        i.putExtra("discussionId", discussionId);
        i.putExtra("endpoint", wsEndpoint);
        i.putExtra("token", session.getToken());
        Log.d("DISCUSSION_FLOW", "Opening LiveDiscussionActivity with ws=" + wsEndpoint);
        startActivity(i);
    }

    // ----------------------------
    // Layout & Inputs
    // ----------------------------

    private boolean tryInflateXml() {
        try {
            setContentView(R.layout.activity_vote);
            // Optional toolbar (won't crash if not present)
            try {
                androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbarVote);
                if (tb != null) setSupportActionBar(tb);
            } catch (Exception ignore) {}

            txtTitle   = findViewById(R.id.txtTitle);
            chipType   = findViewById(R.id.chipType);
            chipStatus = findViewById(R.id.chipStatus);
            container  = findViewById(R.id.container);
            btnSubmit  = findViewById(R.id.btnSubmit);
            btnResults = findViewById(R.id.btnResults);
            boolean ok = container != null && btnSubmit != null && btnResults != null;
            return ok;
        } catch (Exception e) {
            Toast.makeText(this, "Layout error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void buildInputsForType() {
        if (container != null) container.removeAllViews();
        rankRows.clear();

        if (pollType == null) pollType = "";

        if ("YES_NO".equalsIgnoreCase(pollType)) {
            rgYesNo = new RadioGroup(this);
            rgYesNo.setOrientation(RadioGroup.VERTICAL);
            RadioButton rbYes = new RadioButton(this);
            rbYes.setText("Yes");
            rbYes.setId(View.generateViewId());
            RadioButton rbNo = new RadioButton(this);
            rbNo.setText("No");
            rbNo.setId(View.generateViewId());
            rgYesNo.addView(rbYes);
            rgYesNo.addView(rbNo);
            container.addView(rgYesNo);
            return;
        }

        if ("RATING".equalsIgnoreCase(pollType)) {
            ratingBar = new RatingBar(this, null, android.R.attr.ratingBarStyle);
            ratingBar.setNumStars(5);
            ratingBar.setStepSize(1f);
            ratingBar.setIsIndicator(false);
            ratingBar.setClickable(true);
            ratingBar.setFocusable(true);
            ratingBar.setRating(3);
            container.addView(ratingBar);
            return;
        }

        if ("MULTIPLE_CHOICE".equalsIgnoreCase(pollType)) {
            rgChoices = new RadioGroup(this);
            rgChoices.setOrientation(RadioGroup.VERTICAL);
            boolean has = pollOptions != null && !pollOptions.isEmpty();
            if (has) {
                for (String opt : pollOptions) {
                    RadioButton rb = new RadioButton(this);
                    rb.setId(View.generateViewId());
                    rb.setText(opt);
                    rgChoices.addView(rb);
                }
                container.addView(rgChoices);
            } else {
                TextView tv = new TextView(this);
                tv.setText("No options provided.");
                container.addView(tv);
            }
            return;
        }

        if ("RANKING".equalsIgnoreCase(pollType)) {
            boolean has = pollOptions != null && pollOptions.size() > 0;
            if (!has) {
                TextView tv = new TextView(this);
                tv.setText("No options provided for ranking.");
                container.addView(tv);
                return;
            }

            TextView hint = new TextView(this);
            hint.setText("Assign a unique rank to each option:");
            hint.setPadding(0, dp(8), 0, dp(8));
            container.addView(hint);

            int n = pollOptions.size();
            for (String opt : pollOptions) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(0, dp(4), 0, dp(4));

                TextView label = new TextView(this);
                label.setText(opt);
                label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                NumberPicker picker = new NumberPicker(this);
                picker.setMinValue(1);
                picker.setMaxValue(n);
                picker.setWrapSelectorWheel(true);
                picker.setValue(1);

                row.addView(label);
                row.addView(picker);
                container.addView(row);

                RankRow rr = new RankRow();
                rr.option = opt;
                rr.picker = picker;
                rankRows.add(rr);
            }
            return;
        }

        TextView tv = new TextView(this);
        tv.setText("Unsupported poll type: " + (pollType != null ? pollType : ""));
        container.addView(tv);
    }

    // ----------------------------
    // Submit vote
    // ----------------------------

    private void doSubmit() {
        if (pollId <= 0) {
            Toast.makeText(this, "Invalid poll id", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = session.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            String email = session.getEmail();
            userId = (email != null && !email.trim().isEmpty()) ? email : "anonymous@example.com";
        }

        if ("YES_NO".equalsIgnoreCase(pollType)) {
            int checked = (rgYesNo != null) ? rgYesNo.getCheckedRadioButtonId() : -1;
            if (checked == -1) {
                Toast.makeText(this, "Please select Yes or No", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton rb = findViewById(checked);
            boolean isYes = rb != null && "yes".equalsIgnoreCase(rb.getText().toString());

            JSONObject body = new JSONObject();
            try {
                if (!Double.isNaN(latExtra)) body.put("lat", latExtra);
                if (!Double.isNaN(lngExtra)) body.put("lng", lngExtra);
            } catch (Exception ignore) {}

            String url = BASE + "/polls/" + pollId + "/vote?userId=" + encode(userId) + "&numericValue=" + (isYes ? 1 : 0);
            postJson(url, body.toString(), "Vote submitted");
            return;
        }

        if ("RATING".equalsIgnoreCase(pollType)) {
            if (ratingBar == null) {
                Toast.makeText(this, "Rating not ready", Toast.LENGTH_SHORT).show();
                return;
            }
            int numericValue = Math.round(ratingBar.getRating());
            if (numericValue < 1) numericValue = 1;

            JSONObject body = new JSONObject();
            try {
                if (!Double.isNaN(latExtra)) body.put("lat", latExtra);
                if (!Double.isNaN(lngExtra)) body.put("lng", lngExtra);
            } catch (Exception ignore) {}

            String url = BASE + "/polls/" + pollId + "/vote?userId=" + encode(userId) + "&numericValue=" + numericValue;
            postJson(url, body.toString(), "Vote submitted");
            return;
        }

        if ("MULTIPLE_CHOICE".equalsIgnoreCase(pollType)) {
            if (rgChoices == null) {
                Toast.makeText(this, "Choices not ready", Toast.LENGTH_SHORT).show();
                return;
            }
            int checked = rgChoices.getCheckedRadioButtonId();
            if (checked == -1) {
                Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton rb = findViewById(checked);
            String choice = (rb != null) ? rb.getText().toString() : "";
            if (choice == null || choice.trim().isEmpty()) {
                Toast.makeText(this, "Invalid choice", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject body = new JSONObject();
            try {
                if (!Double.isNaN(latExtra)) body.put("lat", latExtra);
                if (!Double.isNaN(lngExtra)) body.put("lng", lngExtra);
            } catch (Exception ignore) {}

            String url = BASE + "/polls/" + pollId + "/vote?userId=" + encode(userId) + "&choice=" + encode(choice);
            postJson(url, body.toString(), "Vote submitted Successfully");
            return;
        }

        if ("RANKING".equalsIgnoreCase(pollType)) {
            if (rankRows.isEmpty()) {
                Toast.makeText(this, "No ranking options to submit", Toast.LENGTH_SHORT).show();
                return;
            }

            int n = rankRows.size();
            boolean[] used = new boolean[n + 1];
            JSONArray arr = new JSONArray();

            for (int i = 0; i < n; i++) {
                RankRow rr = rankRows.get(i);
                int r = rr.picker.getValue();
                if (r < 1 || r > n) {
                    Toast.makeText(this, "Ranks must be between 1 and " + n, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (used[r]) {
                    Toast.makeText(this, "Each rank 1.." + n + " must be used exactly once.", Toast.LENGTH_LONG).show();
                    return;
                }
                used[r] = true;

                JSONObject item = new JSONObject();
                try {
                    item.put("choice", rr.option);
                    item.put("rank", r);
                    if (i == 0) {
                        if (!Double.isNaN(latExtra)) item.put("lat", latExtra);
                        if (!Double.isNaN(lngExtra)) item.put("lng", lngExtra);
                    }
                } catch (Exception ignore) {}
                arr.put(item);
            }

            for (int r = 1; r <= n; r++) {
                if (!used[r]) {
                    Toast.makeText(this, "Please assign all ranks 1.." + n, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            String url = BASE + "/polls/" + pollId + "/vote/ranking?userId=" + encode(userId);
            postJsonArray(url, arr.toString(), "Ranking submitted");
            return;
        }

        Toast.makeText(this, "Unsupported poll type: " + pollType, Toast.LENGTH_SHORT).show();
    }

    // ----------------------------
    // Networking helpers
    // ----------------------------

    private void postJson(String url, String body, String successMsg) {
        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                resp -> Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show(),
                err -> {
                    int code = -1;
                    if (err != null && err.networkResponse != null) code = err.networkResponse.statusCode;
                    String msg = parseVolleyError(err);
                    Toast.makeText(this, "Vote failed (" + code + "): " + msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override public byte[] getBody() { return body.getBytes(StandardCharsets.UTF_8); }
            @Override public String getBodyContentType() { return "application/json; charset=UTF-8"; }
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                return h;
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1.0f));
        AppRequestQueue.get(this).add(req);
    }

    private void postJsonArray(String url, String bodyJsonArray, String successMsg) {
        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                resp -> Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show(),
                err -> {
                    int code = -1;
                    if (err != null && err.networkResponse != null) code = err.networkResponse.statusCode;
                    String msg = parseVolleyError(err);
                    Toast.makeText(this, "Ranking failed (" + code + "): " + msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override public byte[] getBody() { return bodyJsonArray.getBytes(StandardCharsets.UTF_8); }
            @Override public String getBodyContentType() { return "application/json; charset=UTF-8"; }
            @Override public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                return h;
            }
        };
        req.setRetryPolicy(new DefaultRetryPolicy(15000, 0, 1.0f));
        AppRequestQueue.get(this).add(req);
    }

    private String parseVolleyError(com.android.volley.VolleyError err) {
        if (err == null) return "Unknown error";
        if (err instanceof com.android.volley.TimeoutError) return "network timeout";
        if (err instanceof com.android.volley.NoConnectionError) return "no connection";
        String body = "";
        try {
            if (err.networkResponse != null && err.networkResponse.data != null) {
                body = new String(err.networkResponse.data, StandardCharsets.UTF_8);
                JSONObject o = new JSONObject(body);
                String m = o.optString("message", "");
                if (m != null && !m.isEmpty()) return m;
                String e2 = o.optString("error", "");
                if (e2 != null && !e2.isEmpty()) return e2;
            }
        } catch (Exception ignore) {}
        return body.isEmpty() ? err.getClass().getSimpleName() : body;
    }

    // ----------------------------
    // Results & fallback UI
    // ----------------------------

    private void openResults() {
        Intent i = new Intent(this, ResultsActivity.class);
        i.putExtra("pollId", pollId);
        i.putExtra("title", pollTitle);
        i.putExtra("type", pollType);
        if (pollOptions != null) i.putStringArrayListExtra("options", pollOptions);
        startActivity(i);
    }

    private void buildFallbackUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView h = new TextView(this);
        h.setText(pollTitle != null ? pollTitle : "Vote");
        h.setTextSize(20);
        h.setGravity(Gravity.START);

        TextView t = new TextView(this);
        t.setText("Type: " + (pollType != null ? pollType : ""));

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, dp(12), 0, dp(12));

        btnSubmit = new Button(this);
        btnSubmit.setText("Submit");

        btnResults = new Button(this);
        btnResults.setText("See Results");

        // Optional: Live Discussion FAB won't exist in this fallback; that's fine.

        setContentView(new LinearLayout(this) {{
            setOrientation(VERTICAL);
            addView(h);
            addView(t);
            addView(container);
            addView(btnSubmit);
            addView(btnResults);
        }});

        buildInputsForType();
        btnSubmit.setOnClickListener(v -> doSubmit());
        btnResults.setOnClickListener(v -> openResults());
    }

    // ----------------------------
    // Rotate Access Code (owner)
    // ----------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ROTATE_CODE, 0, "Rotate Access Code");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_ROTATE_CODE) {
            onRotateCodeClicked();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onRotateCodeClicked() {
        if (pollId <= 0) {
            Toast.makeText(this, "Poll not ready", Toast.LENGTH_SHORT).show();
            return;
        }
        rotateAccessCode(pollId);
    }

    private void rotateAccessCode(int id) {
        final String url = BASE + "/polls/" + id + "/rotate-code";

        JSONObject body = new JSONObject();
        try {
            String emailId = new SessionManager(this).getEmail();
            if (emailId != null && !emailId.trim().isEmpty()) {
                body.put("emailId", emailId);
            }
        } catch (Exception ignore) {}

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                resp -> {
                    // Try to read the code from the rotate response (if present)
                    String newCode = null;
                    if (resp != null) {
                        newCode = resp.optString("accessCode", null);
                        if (newCode == null && resp.has("poll")) {
                            JSONObject pollObj = resp.optJSONObject("poll");
                            if (pollObj != null) newCode = pollObj.optString("accessCode", null);
                        }
                    }

                    if (newCode != null && !newCode.isEmpty()) {
                        showNewCodeDialog(newCode);
                    } else {
                        // No code returned — fetch it explicitly
                        fetchAccessCode(id);
                    }
                },
                err -> {
                    int code = -1;
                    String msg = "";
                    if (err != null && err.networkResponse != null) {
                        code = err.networkResponse.statusCode;
                        if (err.networkResponse.data != null) {
                            msg = new String(err.networkResponse.data);
                        }
                    }
                    if (code == 403) {
                        Toast.makeText(this, "Only the poll owner can rotate the code.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Rotate failed (" + code + "): " + msg, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                h.put("Content-Type", "application/json");
                return h;
            }
        };

        AppRequestQueue.get(this).add(req);
    }

    private void fetchAccessCode(int pollId) {
        String emailId = new SessionManager(this).getEmail();
        if (emailId == null || emailId.trim().isEmpty()) {
            Toast.makeText(this, "Cannot fetch code: no email in session", Toast.LENGTH_LONG).show();
            return;
        }

        final String url = BASE + "/polls/" + pollId + "/access-code?emailId=" + encode(emailId);

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    try {
                        JSONObject o = new JSONObject(resp);
                        String code = o.optString("accessCode", null);
                        if (code != null && !code.isEmpty()) {
                            showNewCodeDialog(code);
                        } else {
                            Toast.makeText(this, "Rotated, but backend returned no code.", Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to parse access code response", Toast.LENGTH_LONG).show();
                    }
                },
                err -> {
                    int code = -1;
                    String msg = "";
                    if (err != null && err.networkResponse != null) {
                        code = err.networkResponse.statusCode;
                        if (err.networkResponse.data != null) {
                            msg = new String(err.networkResponse.data);
                        }
                    }
                    if (code == 403) {
                        Toast.makeText(this, "Only the owner can retrieve the access code.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Fetch code failed (" + code + "): " + msg, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> h = new HashMap<>();
                h.put("Accept", "application/json");
                return h;
            }
        };

        AppRequestQueue.get(this).add(req);
    }

    private void showNewCodeDialog(String accessCode) {
        final String codeToCopy = accessCode;
        new AlertDialog.Builder(this)
                .setTitle("Access Code Rotated")
                .setMessage("New access code:\n\n" + accessCode)
                .setPositiveButton("Copy", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("Access Code", codeToCopy));
                        Toast.makeText(this, "Access code copied", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    // ----------------------------
    // Misc helpers
    // ----------------------------

    private String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private int dp(int v) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(v * d);
    }
}
