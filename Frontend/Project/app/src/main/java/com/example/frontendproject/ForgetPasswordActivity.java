package com.example.frontendproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;


public class ForgetPasswordActivity extends AppCompatActivity {
    private TextInputEditText EditEmail;
    private TextInputEditText EditCode;
    private Button btnSendCode;
    private Button btnVerify;

    private TextInputEditText editEmail;
    private Button btnSendLink;

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080/users";
    private static final String FORGOT_URL = BASE + "/forgot-password";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forget_password);

        editEmail   = findViewById(R.id.EditTEmail);
        btnSendLink = findViewById(R.id.SendLinkButton);

        btnSendLink.setOnClickListener(v -> sendResetLink());
    }

    private void sendResetLink() {
        String email = "";
        if (editEmail.getText() != null) {
            email = editEmail.getText().toString().trim();
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }
        final String enteredEmail = email;
        String url = FORGOT_URL;
        JSONObject body = new JSONObject();
        try {
            body.put("email", enteredEmail);
        } catch (Exception ignored) {

        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                body,
                response -> {
                    String message = "";
                    try {
                        message = response.optString("message", "");
                    } catch (Exception ignore) {

                    }

                    if (message.isEmpty()) {
                        message = "Password reset link sent (check your email)";
                    }

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, PasswordReset.class);
                    i.putExtra("email", enteredEmail);
                    startActivity(i);
                    finish();
                },
                error -> {
                    int n = -1;
                    String Str = "";
                    if (error != null && error.networkResponse != null) {
                        n = error.networkResponse.statusCode;
                        if (error.networkResponse.data != null) {
                            Str = new String(error.networkResponse.data);
                        }
                    }
                    if (n == 404) {
                        Toast.makeText(this, "Email not found", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to send link " + Str, Toast.LENGTH_LONG).show();
                    }
                }
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("Accept", "application/json");
                h.put("Content-Type", "application/json");
                return h;
            }
        };

        AppRequestQueue.get(this).add(req);
    }


}
