package com.example.frontendproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.frontendproject.net.Api;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * SignupActivity handles user registration.
 *
 * It collects name, email, and password from the signup form,
 * validates the inputs, and sends a POST request to the backend.
 * On success, it navigates the user to OtpVerificationActivity
 * to complete email verification.
 */

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnSignup, btnBack;
    private ProgressBar progress;

    /**
     * Initializes signup UI, binds all input fields and buttons,
     * and sets click listeners for signup and back actions.
     *
     * @param savedInstanceState previous saved state, if any
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make sure your XML file is res/layout/activity_sign_up.xml
        setContentView(R.layout.activity_sign_up);

        // Bind views
        etName     = findViewById(R.id.etName);
        etEmail    = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignup  = findViewById(R.id.btnSignup);
        btnBack    = findViewById(R.id.btnBack);
        progress   = findViewById(R.id.progress);

        btnSignup.setOnClickListener(v -> {
            hideKeyboard();
            doSignUp();
        });

        btnBack.setOnClickListener(v -> finish());
    }
    /**
     * Reads user inputs, validates them, and sends a POST request to create a new user.
     * If the backend returns success, redirects to OTP verification screen.
     */
    private void doSignUp() {
        String name     = safeText(etName).trim();
        String email    = safeText(etEmail).trim();
        String password = safeText(etPassword);

        // Basic validation
        if (name.isEmpty()) {
            toast("Please enter your full name");
            etName.requestFocus();
            return;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            toast("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }
        if (password.length() < 6) {
            toast("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        setLoading(true);

        // Build request body expected by backend: { name, emailId, password }
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("emailId", email);   // <-- IMPORTANT: backend expects emailId
            body.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                Api.CREATE_USER,    // POST http://.../users
                body,
                response -> {
                    setLoading(false);
                    // backend: { "message": "success" }  or { "message": "failure" }
                    String msg = response.optString("message", "");
                    Log.d(TAG, "Signup response: " + response.toString());
                    if ("success".equalsIgnoreCase(msg)) {
                        Toast.makeText(this, "Account created. Check your email for the verification token.", Toast.LENGTH_LONG).show();

                        // Go to token screen (user pastes token from email)
                        Intent i = new Intent(this, OtpVerificationActivity.class);
                        i.putExtra(OtpVerificationActivity.EXTRA_EMAIL, email); // just for display
                        startActivity(i);
                    } else {
                        Toast.makeText(this, msg.isEmpty() ? "Signup failed" : msg, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    setLoading(false);
                    int status = -1;
                    String bodyText = "";
                    NetworkResponse nr = error.networkResponse;
                    if (nr != null && nr.data != null) {
                        status = nr.statusCode;
                        bodyText = new String(nr.data);
                    }
                    Log.e(TAG, "Volley error. status=" + status + " body=" + bodyText, error);
                    Toast.makeText(this, "Network error (" + status + "). Check URL/port.", Toast.LENGTH_LONG).show();
                }
        );

        // Optional: bump timeout a bit for campus server
        req.setRetryPolicy(new DefaultRetryPolicy(
                15000, // 15s
                0,
                1.0f
        ));

        Volley.newRequestQueue(this).add(req);
    } // <--- close doSignUp() properly
    /**
     * Enables/disables form inputs and shows/hides loading spinner while signup is running.
     *
     * @param loading true to show loading state, false to return to normal UI
     */
    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!loading);
        btnBack.setEnabled(!loading);
        etName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }
    /**
     * Safely extracts text from a TextInputEditText.
     *
     * @param et input field
     * @return string value of the field, or empty string if null
     */
    private String safeText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }
    /**
     * Shows a short Toast message on screen.
     *
     * @param msg message to display
     */
    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
    /**
     * Hides the soft keyboard if any input field currently has focus.
     */
    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}


