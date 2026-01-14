package com.example.frontendproject.net;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface DashboardApi {

    @GET("dashboard/polls-joined")
    Call<PollsJoinedResponse> getPollsJoined(
            @Header("Authorization") String authToken
    );

    @GET("dashboard/votes-cast")
    Call<VotesCastResponse> getVotesCast(
            @Header("Authorization") String authToken
    );

    @GET("dashboard/poll-types")
    Call<PollTypesResponse> getPollTypes(
            @Header("Authorization") String authToken
    );

    @GET("dashboard/recent-activity")
    Call<List<RecentActivityItem>> getRecentActivity(
            @Header("Authorization") String authToken
    );
}
