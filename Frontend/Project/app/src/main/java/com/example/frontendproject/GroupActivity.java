package com.example.frontendproject;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupActivity extends AppCompatActivity {

    private static final String BASE = "http://coms-3090-036.class.las.iastate.edu:8080";

    EditText etGroupName, etEmails;
    Button btnCreateGroupFinal;
    ProgressBar progressGroup;

    String creatorEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        SessionManager session = new SessionManager(this);
        creatorEmail = session.getEmail();

        etGroupName = findViewById(R.id.etGroupName);
        etEmails = findViewById(R.id.etEmails);
        btnCreateGroupFinal = findViewById(R.id.btnCreateGroupFinal);
        progressGroup = findViewById(R.id.progressGroup);

        btnCreateGroupFinal.setOnClickListener(v -> createGroup());
    }

    private void loading(boolean on) {
        progressGroup.setVisibility(on ? ProgressBar.VISIBLE : ProgressBar.GONE);
        btnCreateGroupFinal.setEnabled(!on);
    }

    private void createGroup() {
        String name = etGroupName.getText().toString().trim();
        String members = etEmails.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter group name", Toast.LENGTH_SHORT).show();
            return;
        }

        loading(true);

        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("creatorEmail", creatorEmail);
        } catch (JSONException ignored) {}

        JsonObjectRequest createReq = new JsonObjectRequest(
                Request.Method.POST,
                BASE + "/groups",
                body,
                resp -> {
                    try {
                        int groupId = resp.getInt("id");

                        if (!members.isEmpty()) {
                            JSONArray arr = new JSONArray();
                            for (String e : members.split(",")) arr.put(e.trim());

                            JSONObject b2 = new JSONObject();
                            b2.put("memberEmails", arr);

                            JsonObjectRequest addReq = new JsonObjectRequest(
                                    Request.Method.POST,
                                    BASE + "/groups/" + groupId + "/members/batch",
                                    b2,
                                    r -> doneSuccess(),
                                    e -> doneSuccess()
                            );
                            AppRequestQueue.get(this).add(addReq);
                        } else {
                            doneSuccess();
                        }

                    } catch (Exception e) {
                        loading(false);
                    }
                },
                e -> loading(false)
        );

        AppRequestQueue.get(this).add(createReq);
    }

    private void doneSuccess() {
        loading(false);
        Toast.makeText(this, "Group Created :)", Toast.LENGTH_SHORT).show();
        etGroupName.setText("");
        etEmails.setText("");
    }
}
