package com.example.frontendproject;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.frontendproject.net.Api;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class OtpVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    private TextInputLayout tilOtp;
    private TextInputEditText etOtp;
    private Button btnVerify;
    private TextView tvResend, tvTimer, tvSubtitle;
    private ProgressBar progress;

    private String email;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        email = getIntent().getStringExtra(EXTRA_EMAIL);
        if (email == null) email = "";

        tilOtp = findViewById(R.id.tilOtp);
        etOtp = findViewById(R.id.etOtp);
        btnVerify = findViewById(R.id.btnVerify);
        tvResend = findViewById(R.id.tvResend);
        tvTimer = findViewById(R.id.tvTimer);
        tvSubtitle = findViewById(R.id.tvOtpSubtitle);
        progress = findViewById(R.id.progress);

        // Update subtitle to reflect token-based verification
        tvSubtitle.setText("Paste the verification token sent to " + email);

        // Your backend does not provide resend; hide these controls
        tvResend.setVisibility(View.GONE);
        tvTimer.setVisibility(View.GONE);

        btnVerify.setOnClickListener(v -> verifyToken());
    }

    private void verifyToken() {
        String token = etOtp.getText() != null ? etOtp.getText().toString().trim() : "";
        tilOtp.setError(null);

        if (token.isEmpty()) {
            tilOtp.setError("Enter the token from your email");
            return;
        }

        setLoading(true);

        // Build URL: GET /users/verify?token=...
        String url = Api.VERIFY + "?token=" + Uri.encode(token);

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    setLoading(false);
                    // Backend spec: { "message": "Email verified successfully!" } or error message
                    String msg = response.optString("message", "");
                    if (msg.toLowerCase().contains("verified")) {
                        Toast.makeText(this, "Email verified 🎉", Toast.LENGTH_SHORT).show();
                        finish(); // or startActivity(new Intent(this, LoginActivity.class));
                    } else {
                        tilOtp.setError(msg.isEmpty() ? "Invalid or expired token" : msg);
                    }
                },
                error -> {
                    setLoading(false);
                    int status = -1;
                    String body = "";
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null && nr.data != null) {
                        status = nr.statusCode;
                        body = new String(nr.data);
                    }
                    tilOtp.setError(status == -1
                            ? "Network error. Check URL/connection."
                            : "Verification failed (" + status + "). " + body);
                }
        );

        Volley.newRequestQueue(this).add(req);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerify.setEnabled(!loading);
        etOtp.setEnabled(!loading);
    }
}
