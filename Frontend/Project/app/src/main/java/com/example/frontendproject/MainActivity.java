package com.example.frontendproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class MainActivity extends AppCompatActivity {

    private View splash;         // we'll use the root content view
    private VideoView introVideo;

    // Added: gesture detector for a simple double-tap dev shortcut to ChatActivity
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // your ScrollView + VideoView layout

        // Use the root content view as the thing we fade out
        splash = findViewById(android.R.id.content);

        // Your VideoView id in XML is heroVideo (not introVideo)
        introVideo = findViewById(R.id.heroVideo);

        // If you have res/raw/intro.mp4, this will play it. If not, comment these 3 lines.
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.intro);
        introVideo.setVideoURI(videoUri);

        introVideo.setOnPreparedListener(mp -> {
            try { mp.setVolume(0f, 0f); } catch (Exception ignored) {}
            introVideo.start();
        });

        introVideo.setOnCompletionListener(mp -> goNextWithFade());
        introVideo.setOnErrorListener((mp, what, extra) -> {
            goNextWithFade();
            return true;
        });

        // === DEV SHORTCUT #1 (existing): Long-press to open ResultsActivity ===
        introVideo.setOnLongClickListener(v -> {
            Intent test = new Intent(this, ResultsActivity.class);
            // ResultsActivity expects these extras:
            //   - pollId (int)
            //   - title  (String)
            //   - type   (String) -> "YES_NO" | "MULTIPLE_CHOICE" | "RATING" | "RANKING"
            test.putExtra("pollId", 123);                // TODO: replace with a real pollId
            test.putExtra("title", "Demo Poll");         // optional, for header display
            test.putExtra("type", "YES_NO");             // adjust if you want to test other types
            startActivity(test);
            return true;
        });

        // === DEV SHORTCUT #2 (new): Double-tap the video to open ChatActivity ===
        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        startActivity(new Intent(MainActivity.this, ChatActivity.class));
                        return true;
                    }
                });

        // Pass touch events to the gesture detector without breaking video controls
        introVideo.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false; // don't consume; allow normal VideoView behavior
        });
    }

    private void goNextWithFade() {
        if (splash != null) {
            splash.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(this::routeToNext)
                    .start();
        } else {
            routeToNext();
        }
    }

    private void routeToNext() {
        // If logged in -> Profile, else -> Login
        SessionManager session = new SessionManager(this);
        Intent i = session.isLoggedIn()
                ? new Intent(this, ProfileActivity.class)
                : new Intent(this, LoginActivity.class);
        startActivity(i);
        finish();
    }
}

