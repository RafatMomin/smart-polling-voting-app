package com.example.frontendproject;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME   = "smartpolling_prefs";
    private static final String KEY_TOKEN   = "jwt_token";
    private static final String KEY_NAME    = "user_name";
    private static final String KEY_EMAIL   = "user_email";
    private static final String KEY_USERID  = "user_id";
    private static final String KEY_LAST_LAT = "last_lat";
    private static final String KEY_LAST_LNG = "last_lng";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /** Signup-style save: token + name + email */
    public void saveLogin(String token, String name, String email) {
        editor.putString(KEY_TOKEN,  token);
        editor.putString(KEY_NAME,   name);
        editor.putString(KEY_EMAIL,  email);
        editor.apply();
    }

    /** Login-style save: token + userId (if it looks like an email, store as email too) */
    public void saveLogin(String token, String userId) {
        editor.putString(KEY_TOKEN,  token);
        editor.putString(KEY_USERID, userId);
        if (userId != null && userId.contains("@")) {
            editor.putString(KEY_EMAIL, userId);
        }
        editor.apply();
    }

    /** Raw prefs access if needed elsewhere (kept from Harshith’s version) */
    public SharedPreferences getPrefs() { return prefs; }

    public String getToken()  { return prefs.getString(KEY_TOKEN,  null); }
    public String getName()   { return prefs.getString(KEY_NAME,   null); }
    public String getEmail()  { return prefs.getString(KEY_EMAIL,  null); }
    public String getUserId() { return prefs.getString(KEY_USERID, null); }

    public boolean isLoggedIn() { return getToken() != null; }

    public void logout() {
        editor.clear().apply();
    }

    public void saveLastLocation(double lat, double lng) {
        editor.putString(KEY_LAST_LAT, String.valueOf(lat));
        editor.putString(KEY_LAST_LNG, String.valueOf(lng));
        editor.apply();
    }

    public double[] getLastLocation() {
        String la = prefs.getString(KEY_LAST_LAT, null);
        String ln = prefs.getString(KEY_LAST_LNG, null);
        if (la == null || ln == null) return null;
        try {
            return new double[]{ Double.parseDouble(la), Double.parseDouble(ln) };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Convenient setter so other screens can update email post-login */
    public void setEmail(String email) {
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }
}
