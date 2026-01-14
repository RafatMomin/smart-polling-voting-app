package com.example.frontendproject;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.frontendproject.net.DashboardApi;
import com.example.frontendproject.net.PollsJoinedResponse;
import com.example.frontendproject.net.VotesCastResponse;
import com.example.frontendproject.net.PollTypesResponse;
import com.example.frontendproject.net.RecentActivityItem;
import com.example.frontendproject.net.RetrofitClientInstance;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvPollsJoined, tvVotesCast, tvPollTypesDetail, tvRecentActivity;
    private SessionManager session;
    private DashboardApi api;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvPollsJoined = findViewById(R.id.tvPollsJoined);
        tvVotesCast = findViewById(R.id.tvVotesCast);
        tvPollTypesDetail = findViewById(R.id.tvPollTypesDetail);
        tvRecentActivity = findViewById(R.id.tvRecentActivity);

        session = new SessionManager(this);
        token = session.getToken();

        if (token == null || token.trim().isEmpty()) {
            Toast.makeText(this, "No auth token found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }



        Retrofit retrofit = RetrofitClientInstance.getRetrofitInstance();
        api = retrofit.create(DashboardApi.class);

        loadPollsJoined();
        loadVotesCast();
        loadPollTypes();
        loadRecentActivity();
    }

    private void loadPollsJoined() {
        Call<PollsJoinedResponse> call = api.getPollsJoined(token);
        call.enqueue(new Callback<PollsJoinedResponse>() {
            @Override
            public void onResponse(Call<PollsJoinedResponse> call, Response<PollsJoinedResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(DashboardActivity.this, "Polls joined failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                PollsJoinedResponse data = response.body();
                tvPollsJoined.setText("Polls Joined: " + data.count_polls_joined);
            }

            @Override
            public void onFailure(Call<PollsJoinedResponse> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Error loading polls joined", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadVotesCast() {
        Call<VotesCastResponse> call = api.getVotesCast(token);
        call.enqueue(new Callback<VotesCastResponse>() {
            @Override
            public void onResponse(Call<VotesCastResponse> call, Response<VotesCastResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(DashboardActivity.this, "Votes cast failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                VotesCastResponse data = response.body();
                tvVotesCast.setText("Votes Cast: " + data.count_votes_cast);
            }

            @Override
            public void onFailure(Call<VotesCastResponse> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Error loading votes cast", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPollTypes() {
        Call<PollTypesResponse> call = api.getPollTypes(token);
        call.enqueue(new Callback<PollTypesResponse>() {
            @Override
            public void onResponse(Call<PollTypesResponse> call, Response<PollTypesResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(DashboardActivity.this, "Poll types failed (" + response.code() + ")", Toast.LENGTH_SHORT).show();
                    return;
                }
                PollTypesResponse data = response.body();
                String typesText = "Yes/No: " + data.yes_no +
                        "\nRating: " + data.rating +
                        "\nRanking: " + data.ranking +
                        "\nMCQ: " + data.multiple_choice;
                tvPollTypesDetail.setText(typesText);
            }

            @Override
            public void onFailure(Call<PollTypesResponse> call, Throwable t) {
                Toast.makeText(DashboardActivity.this, "Error loading poll types", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecentActivity() {
        Call<List<RecentActivityItem>> call = api.getRecentActivity(token);
        call.enqueue(new Callback<List<RecentActivityItem>>() {
            @Override
            public void onResponse(Call<List<RecentActivityItem>> call, Response<List<RecentActivityItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    tvRecentActivity.setText("No recent activity yet.");
                    return;
                }
                List<RecentActivityItem> list = response.body();
                tvRecentActivity.setText(formatRecentActivity(list));
            }

            @Override
            public void onFailure(Call<List<RecentActivityItem>> call, Throwable t) {
                tvRecentActivity.setText("No recent activity yet.");
            }
        });
    }

    private String formatRecentActivity(List<RecentActivityItem> list) {
        if (list == null || list.isEmpty()) {
            return "No recent activity yet.";
        }
        StringBuilder sb = new StringBuilder();
        for (RecentActivityItem item : list) {
            String title = item.pollTitle != null ? item.pollTitle : "Poll";
            String action = item.action != null ? item.action : "interacted";
            String time = item.time != null ? item.time : "";
            sb.append("• ")
                    .append(action)
                    .append(" on ")
                    .append(title);
            if (!time.isEmpty()) {
                sb.append(" (").append(time).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
