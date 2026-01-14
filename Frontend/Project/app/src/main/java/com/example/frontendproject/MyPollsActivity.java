package com.example.frontendproject;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MyPollsActivity extends AppCompatActivity implements MyPollsAdapter.OnPollLongClickListener {

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080";

    private List<Poll> myPolls = new ArrayList<>();
    private MyPollsAdapter adapter;
    private RecyclerView recycler;
    private ProgressBar progress;

    private ActionMode actionMode;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_my_polls);

        session = new SessionManager(this);

        recycler = findViewById(R.id.recyclerMyPolls);
        progress = findViewById(R.id.progressMyPolls);

        adapter = new MyPollsAdapter(myPolls, this);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        loadPolls();
    }

    private void loadPolls() {
        progress.setVisibility(View.VISIBLE);

        JsonArrayRequest req = new JsonArrayRequest(
                Request.Method.GET,
                BASE + "/polls",
                null,
                arr -> {
                    progress.setVisibility(View.GONE);
                    myPolls.clear();
                    String myEmail = session.getEmail();

                    for (int i = 0; i < arr.length(); i++) {
                        try {
                            JSONObject o = arr.getJSONObject(i);
                            String creator = o.optString("creatorEmail", "");
                            if (myEmail.equalsIgnoreCase(creator)) {
                                Poll p = new Poll();
                                p.id = o.getInt("id");
                                p.title = o.getString("title");
                                p.type = o.getString("type");
                                myPolls.add(p);
                            }
                        } catch (Exception ignore) {}
                    }

                    adapter.notifyDataSetChanged();
                },
                err -> {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load polls", Toast.LENGTH_SHORT).show();
                }
        );

        AppRequestQueue.get(this).add(req);
    }

    @Override
    public void onPollLongPressed() {
        if (actionMode != null) return;

        actionMode = startActionMode(actionModeCallback);
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_delete, menu);
            return true;
        }
        @Override public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.action_delete) {
                adapter.deleteSelectedPolls();
                Toast.makeText(MyPollsActivity.this, "Removed from view (no backend delete)", Toast.LENGTH_SHORT).show();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.clearSelection();
            actionMode = null;
        }
    };
}
