package com.example.frontendproject;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/** Simple Volley RequestQueue singleton */
public class AppRequestQueue {
    private static volatile RequestQueue queue;

    private AppRequestQueue() {}

    public static RequestQueue get(Context context) {
        if (queue == null) {
            synchronized (AppRequestQueue.class) {
                if (queue == null) {
                    queue = Volley.newRequestQueue(context.getApplicationContext());
                }
            }
        }
        return queue;
    }

    public static <T> void add(Request<T> request, Context context) {
        get(context).add(request);
    }
}
