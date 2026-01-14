package com.example.frontendproject.net;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface ChatApi {

    @GET("chat")
    Call<ResponseBody> getChatPublic(@Query("message") String message);

    @GET("chat/auth")
    Call<ResponseBody> getChatAuth(
            @Header("Authorization") String authToken,
            @Query("message") String message
    );

    @GET("chat/history")
    Call<List<ChatHistoryItem>> getHistory(@Header("Authorization") String authToken);

    @GET("chat/stats")
    Call<ChatStats> getStats(@Header("Authorization") String authToken);
}
