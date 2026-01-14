package com.example.frontendproject;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PollingWindow extends AppCompatActivity {

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080";

    private int pollId;
    private String creatorEmail;

    private EditText etWindowName, etStart, etEnd, etEmails;
    private Button btnPickStart, btnPickEnd, btnAddWindow, btnDone, btnCreateGroup;
    private ProgressBar progress;
    private RecyclerView rvGroups;
    private GroupBoxAdapter groupAdapter;

    private final Calendar tmpStart = Calendar.getInstance();
    private final Calendar tmpEnd = Calendar.getInstance();
    private final SimpleDateFormat iso =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polling_window);

        pollId = getIntent().getIntExtra("pollId", -1);
        if (pollId <= 0) { finish(); return; }

        SessionManager session = new SessionManager(this);
        creatorEmail = session.getEmail();

        etWindowName = findViewById(R.id.etWindowName);
        etStart = findViewById(R.id.etStart);
        etEnd = findViewById(R.id.etEnd);
        etEmails = findViewById(R.id.etEmails);
        btnPickStart = findViewById(R.id.btnPickStart);
        btnPickEnd = findViewById(R.id.btnPickEnd);
        btnAddWindow = findViewById(R.id.btnAddWindow);
        btnDone = findViewById(R.id.btnDone);
        btnCreateGroup = findViewById(R.id.btnCreateGroup);
        progress = findViewById(R.id.progress);
        rvGroups = findViewById(R.id.rvGroups);

        rvGroups.setLayoutManager(new LinearLayoutManager(this));
        groupAdapter = new GroupBoxAdapter(new ArrayList<>());
        rvGroups.setAdapter(groupAdapter);

        // Prefill a 30-minute window
        tmpEnd.add(Calendar.MINUTE, 30);
        etStart.setText(iso.format(tmpStart.getTime()));
        etEnd.setText(iso.format(tmpEnd.getTime()));

        // Clicks
        btnPickStart.setOnClickListener(v -> pickDate(true));
        btnPickEnd.setOnClickListener(v -> pickDate(false));
        btnAddWindow.setOnClickListener(v -> createWindow());
        btnDone.setOnClickListener(v -> finish());
        btnCreateGroup.setOnClickListener(v ->
                startActivity(new Intent(PollingWindow.this, GroupActivity.class))
        );

        loadGroups();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGroups();
    }

    private void loading(boolean on) {
        progress.setVisibility(on ? View.VISIBLE : View.GONE);
        btnAddWindow.setEnabled(!on);
        btnDone.setEnabled(!on);
        btnCreateGroup.setEnabled(!on);
    }

    private void loadGroups() {
        loading(true);
        String url = BASE + "/groups/creator/" + creatorEmail;

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET, url, null,
                arr -> {
                    loading(false);
                    List<GroupItem> items = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        items.add(new GroupItem(o.optInt("id"), o.optString("name")));
                    }
                    groupAdapter.setData(items);
                },
                err -> {
                    loading(false);
                    Toast.makeText(this, "Failed to load groups", Toast.LENGTH_SHORT).show();
                }
        );

        AppRequestQueue.get(this).add(req);
    }

    /** Show date & time pickers and write ISO string into the right EditText. */
    private void pickDate(boolean isStart) {
        Calendar c = isStart ? tmpStart : tmpEnd;

        DatePickerDialog dp = new DatePickerDialog(
                this,
                (view, y, m, d) -> {
                    c.set(Calendar.YEAR, y);
                    c.set(Calendar.MONTH, m);
                    c.set(Calendar.DAY_OF_MONTH, d);

                    TimePickerDialog tp = new TimePickerDialog(
                            this,
                            (v2, hour, minute) -> {
                                c.set(Calendar.HOUR_OF_DAY, hour);
                                c.set(Calendar.MINUTE, minute);
                                c.set(Calendar.SECOND, 0);
                                (isStart ? etStart : etEnd).setText(iso.format(c.getTime()));
                            },
                            c.get(Calendar.HOUR_OF_DAY),
                            c.get(Calendar.MINUTE),
                            true
                    );
                    tp.show();
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void createWindow() {
        String name = etWindowName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter window name", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONArray groupIds = new JSONArray();
        for (Integer id : groupAdapter.getSelectedIds()) groupIds.put(id);

        JSONArray individuals = new JSONArray();
        String raw = etEmails.getText().toString().trim();
        if (!raw.isEmpty()) {
            for (String e : raw.split(",")) {
                String s = e.trim();
                if (!s.isEmpty()) individuals.put(s);
            }
        }

        JSONObject body = new JSONObject();
        try {
            body.put("pollId", pollId);
            body.put("windowName", name);
            body.put("creatorEmail", creatorEmail);
            body.put("startTime", etStart.getText().toString().trim());
            body.put("endTime", etEnd.getText().toString().trim());
            if (groupIds.length() > 0) body.put("groupIds", groupIds);
            if (individuals.length() > 0) body.put("individualUserEmails", individuals);
        } catch (Exception ignore) {}

        loading(true);
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, BASE + "/polling-windows", body,
                resp -> {
                    loading(false);
                    Toast.makeText(this, "✅ Polling window added", Toast.LENGTH_SHORT).show();
                    etWindowName.setText("");
                    etEmails.setText("");
                },
                err -> {
                    loading(false);
                    String msg = (err.networkResponse != null && err.networkResponse.data != null)
                            ? new String(err.networkResponse.data) : "Failed to add window";
                    Toast.makeText(this, "ERROR: " + msg, Toast.LENGTH_LONG).show();
                }
        );

        AppRequestQueue.get(this).add(req);
    }

    public static class GroupItem {
        public final int id;
        public final String name;
        public boolean checked = false;
        public GroupItem(int id, String name) { this.id = id; this.name = name; }
    }
}

