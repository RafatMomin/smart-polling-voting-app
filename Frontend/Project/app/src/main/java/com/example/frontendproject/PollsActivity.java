package com.example.frontendproject;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PollsActivity extends AppCompatActivity {

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080";
    private static final String POLLS_URL = BASE + "/polls";
    private static final String JOIN_URL  = BASE + "/polls/join";
    private static final int MENU_JOIN_PRIVATE = 1001;
    private static final int MENU_DASHBOARD = 1002;

    private SessionManager session;

    private RecyclerView recycler;
    private View progress;
    private View empty;
    private TextInputEditText etSearch;
    private FloatingActionButton fabAdd;

    private final List<Poll> all = new ArrayList<>();
    private final List<Poll> filtered = new ArrayList<>();
    private PollsRecycle adapter;

    private double latExtra = Double.NaN;
    private double lngExtra = Double.NaN;

    private int pendingOpenPollId = -1;

    private Chip chipTypeAll, chipTypeYesNo, chipTypeRating, chipTypeMCQ, chipTypeRanking;
    private Chip chipVisAll, chipVisPublic, chipVisPrivate;

    private String currentType = null;
    private String currentVisibility = null;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_polls);

        try {
            androidx.appcompat.widget.Toolbar tb = findViewById(R.id.toolbarPolls);
            if (tb != null) setSupportActionBar(tb);
        } catch (Exception ignore) {}

        session = new SessionManager(this);

        FloatingActionButton fabChat = findViewById(R.id.fabChat);
        if (fabChat != null) {
            fabChat.setOnClickListener(v ->
                    startActivity(new Intent(PollsActivity.this, ChatActivity.class))
            );
        }

        recycler = findViewById(R.id.recyclerPolls);
        progress = findViewById(R.id.progress);
        empty    = findViewById(R.id.emptyState);
        etSearch = findViewById(R.id.etSearch);
        fabAdd   = findViewById(R.id.fabAdd);

        chipTypeAll = findViewById(R.id.chipTypeAll);
        chipTypeYesNo = findViewById(R.id.chipTypeYesNo);
        chipTypeRating = findViewById(R.id.chipTypeRating);
        chipTypeMCQ = findViewById(R.id.chipTypeMCQ);
        chipTypeRanking = findViewById(R.id.chipTypeRanking);

        chipVisAll = findViewById(R.id.chipVisAll);
        chipVisPublic = findViewById(R.id.chipVisPublic);
        chipVisPrivate = findViewById(R.id.chipVisPrivate);

        latExtra = getIntent().getDoubleExtra("lat", Double.NaN);
        lngExtra = getIntent().getDoubleExtra("lng", Double.NaN);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        recycler.setLayoutManager(lm);
        recycler.setHasFixedSize(false);
        recycler.setItemViewCacheSize(20);
        recycler.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        recycler.setNestedScrollingEnabled(true);

        adapter = new PollsRecycle(filtered, p -> {
            if (p.isPrivate() && !p.isMember) {
                showJoinDialogForPoll(p.id);
                return;
            }

            Intent i = new Intent(PollsActivity.this, VoteActivity.class);
            i.putExtra("pollId", p.id);
            i.putExtra("title",  p.title);
            i.putExtra("type",   p.type);
            i.putExtra("token",  session.getToken());

            if (p.choices != null) {
                i.putStringArrayListExtra("options", new ArrayList<>(p.choices));
            }
            if (!Double.isNaN(latExtra)) i.putExtra("lat", latExtra);
            if (!Double.isNaN(lngExtra)) i.putExtra("lng", lngExtra);
            startActivity(i);
        });
        recycler.setAdapter(adapter);

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                @Override public void afterTextChanged(Editable s) {
                    String text = (s != null) ? s.toString() : "";
                    applyFilter(text);
                }
            });
        }

        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Intent i = new Intent(PollsActivity.this, PollsCreate.class);
                i.putExtra("token", session.getToken());
                startActivity(i);
            });
        }

        if (chipTypeAll != null) chipTypeAll.setChecked(true);
        if (chipVisAll != null) chipVisAll.setChecked(true);
        setupFilterListeners();

        loadPolls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPolls();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_JOIN_PRIVATE, 0, "Join Private Poll");
        menu.add(0, MENU_DASHBOARD, 1, "Dashboard");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == MENU_JOIN_PRIVATE) {
            showJoinDialog();
            return true;
        } else if (id == MENU_DASHBOARD) {
            Intent intent = new Intent(PollsActivity.this, DashboardActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showJoinDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter access code (e.g., ABCD1234)");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Join Private Poll")
                .setView(input)
                .setPositiveButton("Join", (d, which) -> {
                    String code = (input.getText() != null) ? input.getText().toString().trim() : "";
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show();
                    } else {
                        joinWithCode(code);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showJoinDialogForPoll(int pollId) {
        final EditText input = new EditText(this);
        input.setHint("Enter access code (e.g., ABCD1234)");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Private Poll")
                .setMessage("This poll is private. Enter the access code to join.")
                .setView(input)
                .setPositiveButton("Join", (d, which) -> {
                    String code = (input.getText() != null) ? input.getText().toString().trim() : "";
                    if (code.isEmpty()) {
                        Toast.makeText(this, "Please enter a code", Toast.LENGTH_SHORT).show();
                    } else {
                        pendingOpenPollId = pollId;
                        joinWithCode(code);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void joinWithCode(String code) {
        try {
            JSONObject body = new JSONObject();
            body.put("code", code);

            String email = (session != null) ? session.getEmail() : null;
            if (email == null || email.trim().isEmpty()) {
                String cand = (session != null) ? session.getUserId() : null;
                if (cand != null && cand.contains("@")) {
                    email = cand;
                    session.setEmail(email);
                }
            }
            if (email == null || email.trim().isEmpty()) {
                Toast.makeText(this, "Set your email first (login or create once).", Toast.LENGTH_LONG).show();
                return;
            }
            body.put("emailId", email);

            if (progress != null) progress.setVisibility(View.VISIBLE);

            com.android.volley.toolbox.JsonObjectRequest req =
                    new com.android.volley.toolbox.JsonObjectRequest(
                            Request.Method.POST,
                            JOIN_URL,
                            body,
                            resp -> {
                                if (progress != null) progress.setVisibility(View.GONE);

                                int joinedId = resp.optInt("pollId", -1);
                                if (pendingOpenPollId == -1 && joinedId != -1) {
                                    pendingOpenPollId = joinedId;
                                }

                                Toast.makeText(this, "Joined! Refreshing polls…", Toast.LENGTH_SHORT).show();
                                loadPolls();
                            },
                            err -> {
                                if (progress != null) progress.setVisibility(View.GONE);
                                int codeNum = -1;
                                String msg = "";
                                if (err != null && err.networkResponse != null) {
                                    codeNum = err.networkResponse.statusCode;
                                    if (err.networkResponse.data != null) {
                                        msg = new String(err.networkResponse.data);
                                    }
                                }
                                if (codeNum == 410) {
                                    Toast.makeText(this, "Code expired. Ask owner for a new one.", Toast.LENGTH_LONG).show();
                                } else if (codeNum == 403) {
                                    Toast.makeText(this, "Invalid or disabled code.", Toast.LENGTH_LONG).show();
                                } else if (codeNum == 429) {
                                    Toast.makeText(this, "Too many attempts. Try later.", Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(this, "Join failed (" + codeNum + "): " + msg, Toast.LENGTH_LONG).show();
                                }
                            }
                    ) {
                        @Override
                        public java.util.Map<String, String> getHeaders() {
                            java.util.Map<String, String> h = new java.util.HashMap<>();
                            h.put("Accept", "application/json");
                            h.put("Content-Type", "application/json");
                            return h;
                        }
                    };

            AppRequestQueue.get(this).add(req);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to build join request", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupFilterListeners() {
        if (chipTypeAll != null) {
            chipTypeAll.setOnClickListener(v -> {
                currentType = null;
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }
        if (chipTypeYesNo != null) {
            chipTypeYesNo.setOnClickListener(v -> {
                currentType = "YES_NO";
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }
        if (chipTypeRating != null) {
            chipTypeRating.setOnClickListener(v -> {
                currentType = "RATING";
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }
        if (chipTypeMCQ != null) {
            chipTypeMCQ.setOnClickListener(v -> {
                currentType = "MULTIPLE_CHOICE";
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }
        if (chipTypeRanking != null) {
            chipTypeRanking.setOnClickListener(v -> {
                currentType = "RANKING";
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }

        if (chipVisAll != null) {
            chipVisAll.setOnClickListener(v -> {
                currentVisibility = null;
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }
        if (chipVisPublic != null) {
            chipVisPublic.setOnClickListener(v -> {
                currentVisibility = "PUBLIC";
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }
        if (chipVisPrivate != null) {
            chipVisPrivate.setOnClickListener(v -> {
                currentVisibility = "PRIVATE";
                String q = (etSearch != null && etSearch.getText() != null)
                        ? etSearch.getText().toString() : "";
                applyFilter(q);
            });
        }
    }

    private void applyFilter(String q) {
        String query = (q != null) ? q.trim().toLowerCase() : "";

        filtered.clear();
        for (Poll p : all) {
            if (currentType != null) {
                if (p.type == null || !currentType.equalsIgnoreCase(p.type)) {
                    continue;
                }
            }
            if (currentVisibility != null) {
                if (p.visibility == null || !currentVisibility.equalsIgnoreCase(p.visibility)) {
                    continue;
                }
            }

            if (!query.isEmpty()) {
                String t = (p.title != null) ? p.title.toLowerCase() : "";
                String ty = (p.type != null) ? p.type.toLowerCase() : "";
                if (!t.contains(query) && !ty.contains(query)) {
                    continue;
                }
            }

            filtered.add(p);
        }

        adapter.notifyDataSetChanged();
        if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void loadPolls() {
        if (progress != null) progress.setVisibility(View.VISIBLE);

        String baseUrl = POLLS_URL;
        List<String> params = new ArrayList<>();

        try {
            String email = (session != null) ? session.getEmail() : null;
            if (email == null || email.trim().isEmpty()) {
                String candidate = (session != null) ? session.getUserId() : null;
                if (candidate != null && candidate.contains("@")) {
                    email = candidate;
                    session.setEmail(email);
                }
            }
            if (email != null && !email.trim().isEmpty()) {
                params.add("emailId=" + Uri.encode(email));
            }
        } catch (Exception ignore) {}

        if (currentType != null) {
            params.add("type=" + Uri.encode(currentType));
        }
        if (currentVisibility != null) {
            params.add("visibility=" + Uri.encode(currentVisibility));
        }

        String url = baseUrl;
        if (!params.isEmpty()) {
            StringBuilder sb = new StringBuilder(baseUrl);
            sb.append("?");
            for (int i = 0; i < params.size(); i++) {
                if (i > 0) sb.append("&");
                sb.append(params.get(i));
            }
            url = sb.toString();
        }

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    if (progress != null) progress.setVisibility(View.GONE);
                    try {
                        JSONArray arr = new JSONArray(resp);
                        all.clear();

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject o = arr.getJSONObject(i);

                            Poll p = new Poll();
                            p.id    = o.optInt("id", -1);
                            p.title = o.optString("title", "");
                            p.type  = o.optString("type",  "");
                            p.now   = o.optBoolean("active", true);

                            p.visibility    = o.optString("visibility", "PUBLIC");
                            p.hasAccessCode = o.optBoolean("hasAccessCode", false);
                            p.isMember      = o.optBoolean("isMember", true);

                            JSONArray opts = o.optJSONArray("options");
                            if (opts != null) {
                                ArrayList<String> list = new ArrayList<>();
                                for (int j = 0; j < opts.length(); j++) {
                                    list.add(opts.optString(j));
                                }
                                p.choices = list;
                            }

                            all.add(p);
                        }

                        String q = (etSearch != null && etSearch.getText() != null)
                                ? etSearch.getText().toString() : "";
                        applyFilter(q);

                        if (pendingOpenPollId != -1) {
                            Poll target = null;
                            for (Poll x : filtered) {
                                if (x.id == pendingOpenPollId) { target = x; break; }
                            }
                            if (target == null) {
                                for (Poll x : all) {
                                    if (x.id == pendingOpenPollId) { target = x; break; }
                                }
                            }
                            if (target != null) {
                                pendingOpenPollId = -1;
                                Intent i = new Intent(PollsActivity.this, VoteActivity.class);
                                i.putExtra("pollId", target.id);
                                i.putExtra("title",  target.title);
                                i.putExtra("type",   target.type);
                                i.putExtra("token",  session.getToken());
                                if (target.choices != null) {
                                    i.putStringArrayListExtra("options", new ArrayList<>(target.choices));
                                }
                                if (!Double.isNaN(latExtra)) i.putExtra("lat", latExtra);
                                if (!Double.isNaN(lngExtra)) i.putExtra("lng", lngExtra);
                                startActivity(i);
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to parse polls", Toast.LENGTH_SHORT).show();
                        if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                },
                err -> {
                    if (progress != null) progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load polls", Toast.LENGTH_LONG).show();
                    if (empty != null) empty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("Accept", "application/json");
                return h;
            }
        };

        AppRequestQueue.get(this).add(req);
    }
}
