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

public class PasswordReset extends AppCompatActivity {

    private TextInputEditText eToken;
    private TextInputEditText eNewPassword;
    private TextInputEditText eConfirmPassword;
    private Button btnResetPassword;
    private String email;

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080/users";
    private static final String RESET_URL = BASE + "/reset-password";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newpassword);

        eToken           = findViewById(R.id.editToken);
        eNewPassword     = findViewById(R.id.editNewPassword);
        eConfirmPassword = findViewById(R.id.editConfirmPassword);
        btnResetPassword = findViewById(R.id.resetPasswordButton);
        String tokenFromIntent = getIntent().getStringExtra("resetToken");
        if (tokenFromIntent != null && !tokenFromIntent.trim().isEmpty()) {
            eToken.setText(tokenFromIntent.trim());
        }

        btnResetPassword.setOnClickListener(v -> doReset());

    }

    private void doReset(){
        String token = "";
        if(eToken.getText() != null){
            token = eToken.getText().toString().trim();
        }
        String newPassword ="";
        if(eNewPassword.getText() != null){
            newPassword = eNewPassword.getText().toString().trim();
        }
        String confirmPassword = "";
        if(eConfirmPassword.getText() !=null){
            confirmPassword= eConfirmPassword.getText().toString().trim();
        }
        token = token.replace("Token:", "")
                .replace("token=", "")
                .replace("\"","")
                .replace("'","")
                .trim();

        if (token.isEmpty()) {
            Toast.makeText(this, "Please enter the reset token from your email", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in both password fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }
        String url = RESET_URL;
        final String tokenF = token;
        final String newPasswordF = newPassword;
        JSONObject body = new JSONObject();
        try {
            body.put("token", tokenF);
            body.put("newPassword", newPasswordF);
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

                    boolean ok = "Password reset successful".equalsIgnoreCase(message)
                            || "ok".equalsIgnoreCase(response.optString("status", ""));

                    if (ok) {
                        Toast.makeText(this, "Password reset successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    } else {
                        if (message.isEmpty()) message = "Password reset failed";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
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
                    Toast.makeText(this, "Reset failed " + Str, Toast.LENGTH_LONG).show();
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

