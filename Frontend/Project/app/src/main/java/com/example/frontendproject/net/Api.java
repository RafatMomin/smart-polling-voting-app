package com.example.frontendproject.net;

public class Api {
    // ← use HTTP (not HTTPS) because that’s how the server is exposed
    public static final String BASE_URL = "http://coms-3090-036.class.las.iastate.edu:8080";

    // Base
    public static final String USERS = BASE_URL + "/users";

    // Endpoints
    // Create user (POST JSON: { "emailId": "...", "password": "...", "name": "..." })
    public static final String CREATE_USER = USERS;

    // Login (GET /users/login/{email}/{password})
    // (we’ll build the full URL at call time because email/password go in the path)
    public static final String LOGIN_BASE = USERS + "/login";

    // Verify (GET /users/verify?token=...)
    public static final String VERIFY = USERS + "/verify";

    // Forgot/Reset
    public static final String FORGOT_PASSWORD = USERS + "/forgot-password"; // PUT ?email=
    public static final String RESET_PASSWORD  = USERS + "/reset-password";  // PUT ?token=&newPassword=
}
