package com.example.frontendproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;


/**
 * MapActivity displays the campus geofence on Google Maps, retrieves the user's current
 * location, and determines voting eligibility based on distance from ISU campus.
 *
 * The activity:
 * 1) Requests location permissions.
 * 2) Gets last known location or requests fresh GPS updates.
 * 3) Shows the user marker and a small personal radius circle on the map.
 * 4) Performs a backend eligibility check using a POST request.
 * 5) Enables the Continue button only if the user is eligible.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final int REQ_LOCATION = 42;

    // === Geofence (keep in sync with backend) ===
    private static final LatLng ISU_CENTER = new LatLng(42.0266, -93.6465);
    private static final float ISU_RADIUS_METERS = 2500f;      // Wallace/Wilson + UV + SUV covered
    private static final float USER_VISUAL_RADIUS_METERS = 60f; // small personal circle
    private static final float DEFAULT_ZOOM = 15f;

    // === Backend ===
    // If you test on Android emulator against a server running on your laptop, use 10.0.2.2
    // Example: "http://10.0.2.2:8080"
    // If the server is remote (Leyla's), keep the remote URL.
    private static final String BASE_URL = "http://coms-3090-036.class.las.iastate.edu:8080";
    //private static final String BASE_URL = "http://10.0.2.2:8080";

    private static final String ELIGIBILITY_PATH = "/location/eligibility";

    // === UI ===
    private GoogleMap gmap;
    private FusedLocationProviderClient fused;
    private TextView tvStatus;
    private Button btnContinue;

    // === Location ===
    private LocationCallback locationCallback;
    private boolean requestingUpdates = false;

    // === Markers/Circles (debounced) ===
    private Marker userMarker;
    private Circle userCircle;
    private Circle campusCircle;

    // === State ===
    private double lastLat = Double.NaN;
    private double lastLng = Double.NaN;
    private boolean insideGeo = false;
    private boolean serverEligible = false;
    private boolean serverCheckedAtLeastOnce = false;

    private SessionManager session; // kept in case you read token elsewhere (not used here)
    /**
     * Initializes the map screen, binds UI components, sets up the map fragment,
     * and prepares the LocationCallback to receive GPS updates.
     *
     * @param savedInstanceState previous saved state, if any
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        tvStatus = findViewById(R.id.tvStatus);
        btnContinue = findViewById(R.id.btnContinue);
        btnContinue.setEnabled(false);
        btnContinue.setOnClickListener(v -> openPolls());

        fused = LocationServices.getFusedLocationProviderClient(this);
        session = new SessionManager(this);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            Toast.makeText(this, "Map fragment not found", Toast.LENGTH_LONG).show();
            tvStatus.setText("Map fragment not found in layout.");
            return;
        }
        mapFragment.getMapAsync(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    handleLocation(loc);
                    // If only one fresh fix is needed for this screen, stop updates
                    stopLocationUpdates();
                }
            }
        };
    }
    /**
     * Called when the Google Map is ready. Configures UI settings,
     * draws the campus geofence circle, and starts permission/location flow.
     *
     * @param googleMap the GoogleMap instance to display and manipulate
     */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gmap = googleMap;

        gmap.getUiSettings().setZoomControlsEnabled(true);
        gmap.getUiSettings().setMyLocationButtonEnabled(true);

        // Draw campus geofence
        if (campusCircle == null) {
            campusCircle = gmap.addCircle(new CircleOptions()
                    .center(ISU_CENTER)
                    .radius(ISU_RADIUS_METERS)
                    .strokeWidth(4f)
                    .strokeColor(0xAA1E88E5)   // semi-opaque blue stroke
                    .fillColor(0x331E88E5));   // translucent fill
            gmap.addMarker(new MarkerOptions().position(ISU_CENTER).title("Campus Center"));
            gmap.getUiSettings().setAllGesturesEnabled(true);

        }

        gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(ISU_CENTER, DEFAULT_ZOOM));
        tvStatus.setText("Map ready. Checking location permission…");

        enableMyLocationIfPermitted();
    }
    /**
     * Checks if the app has location permission.
     * If granted, enables MyLocation layer and fetches location.
     * If not granted, requests permission from the user.
     */
    private void enableMyLocationIfPermitted() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            if (gmap != null) {
                try {
                    gmap.setMyLocationEnabled(true); // blue dot
                } catch (SecurityException ignored) {}
            }
            tvStatus.setText("Location permission granted. Fetching location…");
            getAndShowLocation();
        } else {
            tvStatus.setText("Requesting location permission…");
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION },
                    REQ_LOCATION
            );
        }
    }
    /**
     * Attempts to get the device's last known location.
     * If it is not accurate or recent, starts active GPS updates instead.
     */
    private void getAndShowLocation() {
        fused.getLastLocation()
                .addOnSuccessListener(loc -> {
                    // accept last location only if it's accurate and recent
                    if (isUsable(loc)) {
                        handleLocation(loc);
                    } else {
                        tvStatus.setText("Getting a fresh GPS fix…");
                        startLocationUpdates();
                    }
                })
                .addOnFailureListener(e -> {
                    tvStatus.setText("Location error: " + e.getMessage());
                    // Try active updates as a fallback
                    startLocationUpdates();
                });
    }
    /**
     * Determines if a location fix is usable based on accuracy and recency.
     *
     * @param loc the Location object to evaluate
     * @return true if location is accurate enough and recent, false otherwise
     */
    private boolean isUsable(Location loc) {
        if (loc == null) return false;
        boolean accOK = !loc.hasAccuracy() || loc.getAccuracy() <= 50f; // if no accuracy flag, allow
        long ageMs = Math.abs(System.currentTimeMillis() - loc.getTime());
        boolean fresh = ageMs <= 2 * 60 * 1000L; // <= 2 minutes old
        return accOK && fresh;
    }
    /**
     * Starts requesting high-accuracy GPS location updates until
     * a fresh fix is received or updates are manually stopped.
     */
    @SuppressWarnings("deprecation")

    private void startLocationUpdates() {
        if (requestingUpdates) return;

        boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) return;

        LocationRequest req = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(2000)         // 2s
                .setFastestInterval(1000); // 1s

        requestingUpdates = true;
        fused.requestLocationUpdates(req, locationCallback, getMainLooper());
    }
    /**
     * Stops active GPS updates to avoid unnecessary battery drain.
     */
    private void stopLocationUpdates() {
        if (!requestingUpdates) return;
        fused.removeLocationUpdates(locationCallback);
        requestingUpdates = false;
    }
    /**
     * Updates map marker/circles for the user's location, computes distance
     * from campus center, updates eligibility UI, and triggers backend check.
     *
     * @param loc fresh user location from GPS provider
     */
    private void handleLocation(@NonNull Location loc) {
        lastLat = loc.getLatitude();
        lastLng = loc.getLongitude();

        LatLng me = new LatLng(lastLat, lastLng);

        // Update (not spam) user marker/circle
        if (userMarker == null) {
            userMarker = gmap.addMarker(new MarkerOptions().position(me).title("You"));
        } else {
            userMarker.setPosition(me);
        }
        if (userCircle == null) {
            userCircle = gmap.addCircle(new CircleOptions()
                    .center(me)
                    .radius(USER_VISUAL_RADIUS_METERS)
                    .strokeWidth(3f)
                    .strokeColor(0xAADD8800)   // amber-ish stroke
                    .fillColor(0x33DD8800));   // translucent fill
        } else {
            userCircle.setCenter(me);
        }

        // First fix: moveCamera; later fixes: animate
        if (Double.isNaN(lastLat)) {
            gmap.moveCamera(CameraUpdateFactory.newLatLngZoom(me, 16f));
        } else {
            gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(me, 16f));
        }

        // Client-side distance to campus center
        float[] results = new float[1];
        Location.distanceBetween(me.latitude, me.longitude, ISU_CENTER.latitude, ISU_CENTER.longitude, results);
        float dist = results[0];
        insideGeo = (dist <= ISU_RADIUS_METERS);

        String msg = String.format(Locale.US,
                "You are %.0f m from campus center. %s",
                dist, insideGeo ? "Inside campus geofence." : "Outside campus geofence.");
        tvStatus.setText(msg);

        // Local gate first (fast UX). If server later confirms eligibility, we keep enabled; if server fails, we do NOT disable.
        applyGating();

        // Ask backend once per fresh location
        checkEligibilityWithBackend_NoAuth(lastLat, lastLng);
    }
    /**
     * Enables or disables the Continue button depending on eligibility.
     * Uses local geofence check first, then upgrades eligibility if backend confirms.
     */
    private void applyGating() {
        boolean enable;
        if (insideGeo) {
            enable = true;                         // local pass
        } else {
            enable = serverEligible;               // only if server says eligible
        }
        btnContinue.setEnabled(enable);
        btnContinue.setAlpha(enable ? 1f : 0.5f);

        // Status line decoration
        String base = tvStatus.getText() == null ? "" : tvStatus.getText().toString();
        String suffix;
        if (!serverCheckedAtLeastOnce) {
            suffix = "\nServer: checking…";
        } else {
            suffix = "\nServer: " + (serverEligible ? "✅ Eligible" : "⚠️ Unconfirmed");
        }
        if (!base.endsWith(suffix)) {
            tvStatus.setText(base + suffix);
        }
    }
    /**
     * When returning to the activity, re-checks permissions and refreshes location if needed.
     */

    @Override
    protected void onResume() {
        super.onResume();
        if (gmap != null) enableMyLocationIfPermitted();
    }
    /**
     * Stops GPS updates when the activity is no longer in the foreground.
     */

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }
    /**
     * Receives user response to location permission request.
     * If granted, proceeds to location fetch; otherwise shows fallback message.
     *
     * @param requestCode permission request identifier
     * @param permissions list of requested permissions
     * @param grantResults results for each permission
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            boolean granted = false;
            for (int r : grantResults) {
                if (r == PackageManager.PERMISSION_GRANTED) {
                    granted = true; break;
                }
            }
            Toast.makeText(this, granted ? "Location granted" : "Location denied", Toast.LENGTH_SHORT).show();
            if (granted) {
                enableMyLocationIfPermitted();
            } else {
                tvStatus.setText("Location permission denied. Map shows campus, but blue dot is disabled.");
            }
        }
    }
    /**
     * Opens PollsActivity if it exists. Passes user location and geofence state
     * through Intent extras. If PollsActivity is not found, shows a dialog instead.
     */
    private void openPolls() {
        try {
            Class<?> polls = Class.forName("com.example.frontendproject.PollsActivity");
            Intent i = new Intent(this, polls);
            if (!Double.isNaN(lastLat))  i.putExtra("lat", lastLat);
            if (!Double.isNaN(lastLng))  i.putExtra("lng", lastLng);
            i.putExtra("insideGeo", insideGeo);
            startActivity(i);
        } catch (ClassNotFoundException e) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Eligible to Vote")
                    .setMessage("You are inside the campus geofence.\n\nThis is where the Polling screen will open in the final app.")
                    .setPositiveButton("OK", null)
                    .show();
        }
    }


    /**
     * Sends the user's latitude and longitude to the backend eligibility endpoint.
     * Updates UI based on backend eligibility response.

     * Endpoint: POST /location/eligibility (no JWT required)
     *
     * @param lat user's latitude
     * @param lng user's longitude
     */
    private void checkEligibilityWithBackend_NoAuth(double lat, double lng) {
        final String url = BASE_URL + ELIGIBILITY_PATH;
        final String body = "{\"lat\":" + lat + ",\"lng\":" + lng + "}";

        Log.d(TAG, "POST " + url + " body=" + body);

        StringRequest req = new StringRequest(
                Request.Method.POST,
                url,
                resp -> {
                    serverCheckedAtLeastOnce = true;
                    try {
                        JSONObject o = new JSONObject(resp);
                        boolean eligible = o.optBoolean("eligible", false);
                        int dist = (int) Math.round(o.optDouble("distanceMeters", -1));
                        serverEligible = eligible;

                        // Update status line (no toasts here)
                        String base = tvStatus.getText() == null ? "" : tvStatus.getText().toString();
                        String serverLine = "Server: " + (eligible ? "✅ Eligible" : "❌ Not eligible");
                        if (dist >= 0) serverLine += " (" + dist + " m)";
                        tvStatus.setText(base.replaceAll("\\nServer:.*$", "") + "\n" + serverLine);

                        // If user was outside locally but server upgrades to eligible, enable
                        applyGating();
                    } catch (Exception e) {
                        tvStatus.setText(appendOrReplaceServerLine(tvStatus.getText().toString(), "Server: ⚠️ Unconfirmed"));
                        applyGating();
                    }
                },
                err -> {
                    serverCheckedAtLeastOnce = true;
                    Log.w(TAG, "Volley error: " + err);
                    tvStatus.setText(appendOrReplaceServerLine(tvStatus.getText().toString(), "Server: ⚠️ Unconfirmed"));
                    applyGating();
                }
        ) {
            @Override
            public byte[] getBody() {
                return body.getBytes(StandardCharsets.UTF_8);
            }
            @Override
            public String getBodyContentType() {
                return "application/json; charset=UTF-8";
            }
        };

        // Faster feedback while testing
        req.setRetryPolicy(new DefaultRetryPolicy(
                4000, // 4s timeout
                0,    // no retries
                1f
        ));

        AppRequestQueue.get(this).add(req);
    }
    /**
     * Utility helper to update (replace or append) the "Server:" line
     * in the status TextView without duplicating it.
     *
     * @param current current status text
     * @param serverLine new server status line to place
     * @return updated status text including the new server line
     */

    static String appendOrReplaceServerLine(String current, String serverLine) {
        if (current == null) return serverLine;
        if (current.contains("\nServer:")) {
            return current.replaceAll("\\nServer:.*$", "\n" + serverLine);
        } else {
            return current + "\n" + serverLine;
        }
    }
}
