package com.example.frontendproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText editEmail;
    private TextInputEditText editPassword;
    private Button btnLogin;
    private Button btnSignup;
    private Button btnForget;
    private SessionManager session;
    private static final String LOGIN_URL =
            "http://coms-3090-036.class.las.iastate.edu:8080/users/login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_screen);

        session = new SessionManager(this);

        editEmail = findViewById(R.id.emailaddresslogin);
        editPassword = findViewById(R.id.passwordlogin);
        btnLogin = findViewById(R.id.loginbutton);
        btnSignup = findViewById(R.id.signupButton);
        btnForget = findViewById(R.id.forgetPasswordButton);

        btnLogin.setOnClickListener(v -> doLogin());
        btnSignup.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
        btnForget.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, ForgetPasswordActivity.class))
        );
    }

    private void doLogin() {
        String e = "";
        if (editEmail.getText() != null) {
            e = editEmail.getText().toString().trim();
        }
        String p = "";
        if (editPassword.getText() != null) {
            p = editPassword.getText().toString().trim();
        }
        if (e.isEmpty() || p.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        final String email = e;
        final String password = p;

        String url = LOGIN_URL;
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (JSONException ignored) {

        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                url,
                body,
                response -> {
                    boolean success = false;
                    String token = null;
                    String userId = "";

                    try {
                        String t1 = response.optString("token", null);
                        if (t1 != null && !t1.isEmpty()) {
                            token = t1;
                        } else {
                            String t2 = response.optString("accessToken", null);
                            if (t2 != null && !t2.isEmpty()) {
                                token = t2;
                            }
                        }

                        JSONObject user = response.optJSONObject("user");
                        if (user != null) {
                            String id1 = user.optString("id", "");
                            if (!id1.isEmpty()) {
                                userId = id1;
                            }
                            if (userId.isEmpty()) {
                                String id2 = user.optString("userId", "");
                                if (!id2.isEmpty()) userId = id2;
                            }
                        }
                        if (userId.isEmpty()) {
                            String id3 = response.optString("userId", "");
                            if (!id3.isEmpty()) userId = id3;
                        }

                        String msg = response.optString("message", "");
                        if ("Login successful".equalsIgnoreCase(msg)){
                            success = true;
                        }
                    } catch (Exception ignore) {

                    }

                    if (token != null){
                        success = true;
                    }

                    if (success) {
                        if (userId.isEmpty()) {
                            userId = email;
                        }
                        String tokenTobeSaved ="";
                        if(token != null){
                            tokenTobeSaved = token;
                        }
                        session.saveLogin(tokenTobeSaved, userId);
                        Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                        finish();
                    } else {
                        try {
                            String msg = response.optString("message", "");
                            if (!msg.isEmpty()) {
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (Exception ignore) {}
                        Toast.makeText(LoginActivity.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    int n = -1;
                    if (error != null && error.networkResponse != null) n = error.networkResponse.statusCode;
                    String Str = "";
                    if (error != null && error.networkResponse != null && error.networkResponse.data != null) {
                        Str = new String(error.networkResponse.data);
                    }
                    Toast.makeText(LoginActivity.this, "Login failed " + Str, Toast.LENGTH_LONG).show();
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

