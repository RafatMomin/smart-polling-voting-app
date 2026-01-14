package com.example.frontendproject;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

@RunWith(AndroidJUnit4.class)
public class AppRequestQueueTest {

    @Test
    public void get_returnsSingletonInstance() {
        Context ctx = ApplicationProvider.getApplicationContext();
        RequestQueue first = AppRequestQueue.get(ctx);
        RequestQueue second = AppRequestQueue.get(ctx);

        assertNotNull(first);
        assertNotNull(second);
        assertSame(first, second);
    }

    @Test
    public void add_addsRequestToQueue() {
        Context ctx = ApplicationProvider.getApplicationContext();
        RequestQueue queue = AppRequestQueue.get(ctx);

        StringRequest req = new StringRequest(
                Request.Method.GET,
                "http://example.com",
                response -> {},
                error -> {}
        );

        AppRequestQueue.add(req, ctx);

        assertNotNull(queue);
    }
}
