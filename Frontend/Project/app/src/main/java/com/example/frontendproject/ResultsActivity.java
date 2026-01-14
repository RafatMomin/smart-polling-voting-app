package com.example.frontendproject;

import static android.content.Intent.getIntent;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.LifecycleEvent;

public class ResultsActivity extends AppCompatActivity {

    private static final String TAG = "ResultsActivity";

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080";
    private static final String HTTP_BASE = "http://coms-3090-036.class.las.iastate.edu:8080";
    private static final String WS_URL = "ws://coms-3090-036.class.las.iastate.edu:8080/ws/live-results/websocket";

    private int pollId = -1;
    private String pollTitle = "";
    private String pollType = "";

    private TextView txtTitleResults;
    private TextView txtResults;

    private TextView tvStatus, tvStream, tvMeta;
    private Button btnSeeResults, btnSendDummyVote;
    private PieChart pieChart;
    private BarChart barChart;
    private RecyclerView listRanking;

    private StompClient stompClient;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private volatile boolean connected = false;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_results);

        pollId = getIntent().getIntExtra("pollId", -1);
        pollTitle = getIntent().getStringExtra("title");
        pollType = getIntent().getStringExtra("type");

        txtTitleResults = findViewById(R.id.txtTitleResults);
        txtResults = findViewById(R.id.txtResults);

        tvStatus = findViewById(R.id.tvStatus);
        tvStream = findViewById(R.id.tvOutput);
        tvMeta = findViewById(R.id.tvMeta);
        btnSeeResults = findViewById(R.id.btnSeeResults);


        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        listRanking = findViewById(R.id.listRanking);

        if (listRanking != null) {
            listRanking.setLayoutManager(new LinearLayoutManager(this));
        }

        if (btnSeeResults != null) btnSeeResults.setEnabled(false);
        if (btnSendDummyVote != null) btnSendDummyVote.setEnabled(false);

        if (txtTitleResults != null) {
            String t = "";
            if (pollTitle != null) t = pollTitle;
            if (pollType != null && !pollType.isEmpty()) {
                t = t + "  •  " + pollType.replace('_', ' ');
            }
            txtTitleResults.setText(t);
        }

        loadResults();
        connectStomp();

        if (btnSeeResults != null) {
            btnSeeResults.setOnClickListener(v -> subscribeToResults());
        }
        if (btnSendDummyVote != null) {
            btnSendDummyVote.setOnClickListener(v -> sendDummyVote());
        }
    }

    private void connectStomp() {
        if (tvStatus != null) tvStatus.setText("Connecting to live results…");

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL);
        stompClient.withClientHeartbeat(10000);
        stompClient.withServerHeartbeat(10000);

        disposables.add(
                stompClient.lifecycle()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(lce -> {
                            if (lce.getType() == LifecycleEvent.Type.OPENED) {
                                connected = true;
                                if (tvStatus != null) tvStatus.setText("Connected ✅");
                                if (btnSeeResults != null) btnSeeResults.setEnabled(true);
                                if (btnSendDummyVote != null) btnSendDummyVote.setEnabled(true);
                            } else if (lce.getType() == LifecycleEvent.Type.ERROR) {
                                connected = false;
                                if (tvStatus != null) tvStatus.setText("WS error");
                                Log.e(TAG, "WS error", lce.getException());
                            } else if (lce.getType() == LifecycleEvent.Type.CLOSED) {
                                connected = false;
                                if (tvStatus != null) tvStatus.setText("Disconnected ❌");
                                if (btnSeeResults != null) btnSeeResults.setEnabled(false);
                                if (btnSendDummyVote != null) btnSendDummyVote.setEnabled(false);
                            }
                        }, err -> {
                            connected = false;
                            if (tvStatus != null) tvStatus.setText("Lifecycle error");
                            Log.e(TAG, "Lifecycle error", err);
                        })
        );

        stompClient.connect();
    }

    private void subscribeToResults() {
        if (!connected) {
            if (tvStatus != null) tvStatus.setText("Not connected yet…");
            return;
        }

        fetchInitialResults();

        String topic = "/topic/poll/" + pollId;
        if (tvStatus != null) tvStatus.setText("Subscribed to " + topic);

        disposables.add(
                stompClient.topic(topic)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(msg -> {
                            try {
                                JSONObject o = new JSONObject(msg.getPayload());
                                renderLiveResultsMessage(o);
                                try { renderResults(o); } catch (Exception ignored) {}
                            } catch (Exception e) {
                                if (tvStatus != null) tvStatus.setText("Parse error");
                            }
                        }, err -> {
                            if (tvStatus != null) tvStatus.setText("Subscribe error");
                            Log.e(TAG, "subscribe error", err);
                        })
        );
    }

    private void fetchInitialResults() {
        if (pollId <= 0) return;

        String url = HTTP_BASE + "/live-results/poll/" + pollId;

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    try {
                        JSONObject initial = new JSONObject(resp);
                        renderLiveResultsMessage(initial);
                        try { renderResults(initial); } catch (Exception ignored) {}
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to parse initial results", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> Toast.makeText(this, "Failed to fetch initial results", Toast.LENGTH_SHORT).show()
        );

        AppRequestQueue.get(this).add(req);
    }

    private void sendDummyVote() {
        if (!connected) {
            if (tvStatus != null) tvStatus.setText("Not connected yet…");
            return;
        }
        try {
            JSONObject vote = new JSONObject();
            vote.put("choiceId", 1);
            vote.put("userId", 999);

            String dest = "/app/polls/" + pollId + "/vote";
            disposables.add(
                    stompClient.send(dest, vote.toString())
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(() -> {
                                if (tvStatus != null) tvStatus.setText("Vote sent ✅");
                            }, err -> {
                                if (tvStatus != null) tvStatus.setText("Send error");
                            })
            );
        } catch (Exception e) {
            if (tvStatus != null) tvStatus.setText("Vote JSON error");
        }
    }

    private void renderLiveResultsMessage(JSONObject o) throws Exception {
        int total = o.optInt("totalVotes", 0);
        String status = o.optString("status", "—");

        if (tvMeta != null) {
            tvMeta.setText("Total: " + total + "   •   Status: " + status);
        }

        JSONArray dist = o.getJSONArray("distribution");
        ArrayList<String> labels = new ArrayList<>();
        ArrayList<BarEntry> bars = new ArrayList<>();
        ArrayList<PieEntry> pies = new ArrayList<>();

        for (int i = 0; i < dist.length(); i++) {
            JSONObject d = dist.getJSONObject(i);
            int optionId = d.getInt("optionId");
            int count = d.getInt("count");
            double pct = d.optDouble("percent", -1);

            String label = "Option " + optionId;
            labels.add(label);
            bars.add(new BarEntry(i, (float) count));

            if (pct >= 0) pies.add(new PieEntry((float) pct, label));
            else pies.add(new PieEntry((float) count, label));
        }

        showBar(labels, bars);
    }

    private void showPie(ArrayList<PieEntry> entries) {
        if (pieChart == null || barChart == null) return;

        pieChart.setVisibility(android.view.View.VISIBLE);
        barChart.setVisibility(android.view.View.GONE);

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setSliceSpace(2f);
        ds.setValueTextSize(12f);

        PieData data = new PieData(ds);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.animateY(400);
        pieChart.invalidate();
    }

    private void showBar(ArrayList<String> labels, ArrayList<BarEntry> entries) {
        if (pieChart == null || barChart == null) return;

        pieChart.setVisibility(android.view.View.GONE);
        barChart.setVisibility(android.view.View.VISIBLE);

        BarDataSet ds = new BarDataSet(entries, "");
        ds.setValueTextSize(12f);

        BarData data = new BarData(ds);
        data.setBarWidth(0.6f);

        barChart.setData(data);

        XAxis x = barChart.getXAxis();
        x.setValueFormatter(new IndexAxisValueFormatter(labels));
        x.setGranularity(1f);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);

        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.animateY(400);
        barChart.invalidate();
    }

    private void loadResults() {
        if (pollId <= 0) {
            Toast.makeText(this, "Invalid poll id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String url = BASE + "/polls/" + pollId + "/results";

        StringRequest req = new StringRequest(
                Request.Method.GET,
                url,
                resp -> {
                    try {
                        JSONObject o = new JSONObject(resp);
                        renderResults(o);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to parse results", Toast.LENGTH_SHORT).show();
                    }
                },
                err -> {
                    int code = -1;
                    String body = "";
                    if (err != null && err.networkResponse != null) {
                        code = err.networkResponse.statusCode;
                        if (err.networkResponse.data != null) {
                            body = new String(err.networkResponse.data);
                        }
                    }
                    Toast.makeText(this, "Failed to load results (" + code + "): " + body, Toast.LENGTH_LONG).show();
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

    private void renderResults(JSONObject o) {
        String t = "";
        if (pollType != null) t = pollType;

        if ("YES_NO".equalsIgnoreCase(t)) {
            int yes = o.optInt("yes", 0);
            int no = o.optInt("no", 0);
            int total = o.optInt("total", yes + no);
            double pct = total == 0 ? 0.0 : (yes * 100.0) / total;

            String text = "Yes: " + yes + "\n"
                    + "No: " + no + "\n"
                    + "Total: " + total + "\n"
                    + "Yes %: " + pct;
            if (txtResults != null) txtResults.setText(text);
            return;
        }

        if ("RATING".equalsIgnoreCase(t)) {
            int total = o.optInt("totalVotes", o.optInt("total", 0));
            double avg = o.optDouble("average", 0.0);
            int max = o.optInt("max", 5);

            String text = "Average: " + avg + " / " + max + "\n"
                    + "Total votes: " + total;
            if (txtResults != null) txtResults.setText(text);
            return;
        }

        if ("MULTIPLE_CHOICE".equalsIgnoreCase(t)) {
            StringBuilder sb = new StringBuilder();
            int total = o.optInt("totalVotes", 0);
            sb.append("Total votes: ").append(total).append("\n");

            JSONArray breakdown = o.optJSONArray("breakdown");
            if (breakdown != null) {
                for (int i = 0; i < breakdown.length(); i++) {
                    JSONObject row = breakdown.optJSONObject(i);
                    if (row != null) {
                        String option = row.optString("option", "");
                        int count = row.optInt("count", 0);
                        double percent = row.optDouble("percent", 0);
                        sb.append(option)
                                .append(": ")
                                .append(count)
                                .append(" (")
                                .append(percent)
                                .append("%)")
                                .append("\n");
                    }
                }
            }
            if (txtResults != null) txtResults.setText(sb.toString());
            return;
        }

        if ("RANKING".equalsIgnoreCase(t)) {
            StringBuilder sb = new StringBuilder();
            int total = o.optInt("totalVotes", 0);
            sb.append("Total votes: ").append(total).append("\n");

            JSONArray scores = o.optJSONArray("scores");
            if (scores != null) {
                for (int i = 0; i < scores.length(); i++) {
                    JSONObject row = scores.optJSONObject(i);
                    if (row != null) {
                        String choice = row.optString("choice", "");
                        int score = row.optInt("score", 0);
                        sb.append(choice).append(": ").append(score).append("\n");
                    }
                }
            }
            if (txtResults != null) txtResults.setText(sb.toString());
            return;
        }

        if (txtResults != null) txtResults.setText(o.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.dispose();
        if (stompClient != null) stompClient.disconnect();
    }
}
