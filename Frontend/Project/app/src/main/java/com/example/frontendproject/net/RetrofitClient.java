package com.example.frontendproject.net;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Use Rohan’s host (note the trailing slash)
    private static final String BASE_URL = "http://coms-3090-036.class.las.iastate.edu:8080/";

    private static Retrofit retrofit;

    public static Retrofit get() {
        if (retrofit == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient ok = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(ok)
                    // Gson is only used when a method returns a POJO; plain text calls still use ResponseBody
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static ChatApi chat() {
        return get().create(ChatApi.class);
    }
}
