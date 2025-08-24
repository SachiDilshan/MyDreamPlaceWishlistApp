package com.s92086882.mydreamplacewishlist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detail/Edit screen for a single Dream Place.
 * -
 * Shows:
 * - Name, City, Notes
 * - Horizontal photo gallery (tap → full screen view / delete)
 * - Visited toggle + optional rating
 * - Embedded map pointing to the saved coordinates
 * -
 * Saves:
 * - Guest: updates SQLite via DreamPlaceSQLiteHelper
 * - Logged-in: updates Firestore document (and keeps photos list in sync)
 * -
 * Notes:
 * - Intent extra key may be "dream_place" (Search) or "dreamPlace" (Home).
 * - MapView uses manual lifecycle forwarding (onResume/onPause/…).
 * - Firestore field name used below is "photoPaths"; creation code elsewhere uses "photos".
 *   Keep your reads/writes consistent app-wide to avoid mismatches.
 */
public class MyDreamPlaceActivity extends AppCompatActivity {

    // UI Components
    private EditText nameEditText, cityEditText, notesEditText;
    private RecyclerView photoRecyclerView;
    private CheckBox visitedCheckBox;
    private RatingBar ratingBar;
    private Button saveButton;

    // Adapters and data
    private PhotoAdapter photoAdapter;
    private DreamPlace dreamPlace; // model passed in via Intent
    private List<Uri> photoUris; // working list of image URIs (for adapter)

    // --- Auth / DB helpers ---
    private boolean isGuest;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private DreamPlaceSQLiteHelper dbHelper;

    // Google Map
    private MapView mapView;
    private GoogleMap googleMap;

    // --- Result handling for full-screen photo viewer ---
    private static final int REQUEST_PHOTO_VIEW = 101;
    private int lastViewedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_dream_place);

        // Add top padding for status bar to avoid status bar overlap
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        // Init Firebase and local database
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        dbHelper = new DreamPlaceSQLiteHelper(this);
        isGuest = getSharedPreferences("auth", MODE_PRIVATE).getBoolean("isGuest", true);

        initViews();
        setupToolbar();
        loadPlaceDataFromIntent(); // populate UI + map + photos
        setupListeners();
    }

    /** Bind view references and basic Recycler/Map setup */
    private void initViews() {
        nameEditText = findViewById(R.id.editTextName);
        cityEditText = findViewById(R.id.editTextCity);
        notesEditText = findViewById(R.id.editTextNotes);
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        visitedCheckBox = findViewById(R.id.checkboxVisited);
        ratingBar = findViewById(R.id.ratingBar);
        saveButton = findViewById(R.id.saveButton);

        // Horizontal photo strip
        photoRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // MapView requires explicit lifecycle forwarding; using null for saved state (keeps it simple)
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(null);
    }

    /** Configure toolbar (back button + share menu) */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        // Share button lives in menu_share.xml
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                sharePlace();
                return true;
            }
            return false;
        });
    }

    /** Inflate the share menu into the toolbar */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        return true;
    }

    /**
     * Read DreamPlace passed via Intent and hydrate the UI + map + gallery.
     * - Supports two extras names: "dream_place" (from Search) or "dreamPlace" (from Home).
     * - Prepares PhotoAdapter and click-to-view/delete flow.
     * - Sets up MapView camera + marker to this place.
     */
    private void loadPlaceDataFromIntent() {
        Intent intent = getIntent();
        // Some parts of the app use "dream_place" (e.g., Search results)
        if (intent != null && intent.hasExtra("dream_place")) {
            dreamPlace = (DreamPlace) intent.getSerializableExtra("dream_place");
        }

        // Try "dreamPlace" (used in Home)
        if (dreamPlace == null && intent.hasExtra("dreamPlace")) {
            dreamPlace = (DreamPlace) intent.getSerializableExtra("dreamPlace");
        }

            if (dreamPlace != null) {
                // Set basic fields
                nameEditText.setText(dreamPlace.getName());
                cityEditText.setText(dreamPlace.getCity());
                notesEditText.setText(dreamPlace.getNotes());
                // Set visited status and optional rating
                visitedCheckBox.setChecked(dreamPlace.isVisited());
                ratingBar.setRating(dreamPlace.getRating());
                ratingBar.setVisibility(dreamPlace.isVisited() ? View.VISIBLE : View.GONE);

                // Load photo URIs into adapter
                photoUris = new ArrayList<>();
                for (String uriStr : dreamPlace.getPhotoPaths()) {
                    photoUris.add(Uri.parse(uriStr));
                }

                photoAdapter = new PhotoAdapter(photoUris);
                photoRecyclerView.setAdapter(photoAdapter);

                // On photo tap → open full screen viewer (with potential delete)
                photoAdapter.setOnPhotoClickListener((position, uri) -> {
                    lastViewedPosition = position;
                    Intent viewerIntent = new Intent(MyDreamPlaceActivity.this, PhotoViewerActivity.class);
                    viewerIntent.putExtra("photo_uri", uri.toString());
                    startActivityForResult(viewerIntent, REQUEST_PHOTO_VIEW);
                });

                // Show location on map
                mapView.getMapAsync(map -> {
                    googleMap = map;
                    MapsInitializer.initialize(this);
                    googleMap.getUiSettings().setZoomControlsEnabled(true);
                    LatLng location = new LatLng(dreamPlace.getLatitude(), dreamPlace.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(location).title(dreamPlace.getName()));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
                });
        }
    }

    /** Wire interactive bits (toggle rating visibility, Save button) */
    private void setupListeners() {
        // Show rating bar only if "Visited" is checked
        visitedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ratingBar.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        saveButton.setOnClickListener(v -> saveUpdatedPlace());
    }

    /**
     * Collect edits from UI, write back to local DB (guest) or Firestore (logged-in).
     * - Also syncs photo list in memory with adapter contents.
     * - Validates required fields (name + city).
     */
    private void saveUpdatedPlace() {
        String name = nameEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();
        boolean visited = visitedCheckBox.isChecked();
        float rating = visited ? ratingBar.getRating() : 0;

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(city)) {
            Toast.makeText(this, "Name and City are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update in-memory model first
        dreamPlace.setName(name);
        dreamPlace.setCity(city);
        dreamPlace.setNotes(notes);
        dreamPlace.setVisited(visited);
        dreamPlace.setRating(rating);

        // Update photo paths (after deletion)
        List<String> updatedPhotoPaths = new ArrayList<>();
        for (Uri uri : photoUris) {
            updatedPhotoPaths.add(uri.toString());
        }
        dreamPlace.setPhotoPaths(updatedPhotoPaths);

        if (isGuest) {
            dbHelper.updateDreamPlace(dreamPlace);
            Toast.makeText(this, "Updated locally (guest)", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            String uid = mAuth.getCurrentUser().getUid();

            if (dreamPlace.getId() == null) {
                Toast.makeText(this, "Missing place ID. Cannot update.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare partial update map
            Map<String, Object> updated = new HashMap<>();
            updated.put("name", name);
            updated.put("city", city);
            updated.put("notes", notes);
            updated.put("visited", visited);
            updated.put("rating", rating);
            updated.put("photoPaths", updatedPhotoPaths);

            firestore.collection("users").document(uid)
                    .collection("dream_places").document(dreamPlace.getId())
                    .update(updated)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Updated in Firestore", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /** Share this place via a text intent (includes Google Maps link) */
    private void sharePlace() {
        if (dreamPlace == null) return;

        String googleMapsLink = "https://www.google.com/maps/search/?api=1&query="
                + dreamPlace.getLatitude() + "," + dreamPlace.getLongitude();

        String text = "Check out this place!\n" +
                "Name: " + dreamPlace.getName() + "\n" +
                "City: " + dreamPlace.getCity() + "\n" +
                "Notes: " + dreamPlace.getNotes() + "\n" +
                "Location: " + googleMapsLink;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    /**
     * Handle return from PhotoViewerActivity.
     * - If user deleted a photo there, remove it here, notify adapter, and persist change.
     * - If no more photos remain, we optionally finish() to return to previous screen.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PHOTO_VIEW && resultCode == RESULT_OK && data != null) {
            if (data.hasExtra("delete_photo_uri")) {
                Uri deletedUri = Uri.parse(data.getStringExtra("delete_photo_uri"));
                if (photoUris.remove(deletedUri)) {
                    photoAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();

                    // Also update dreamPlace and save it
                    List<String> updatedPhotoPaths = new ArrayList<>();
                    for (Uri uri : photoUris) {
                        updatedPhotoPaths.add(uri.toString());
                    }
                    dreamPlace.setPhotoPaths(updatedPhotoPaths);

                    // Save immediately after deletion
                    if (isGuest) {
                        dbHelper.updateDreamPlace(dreamPlace);
                    } else {
                        String uid = mAuth.getCurrentUser().getUid();
                        firestore.collection("users").document(uid)
                                .collection("dream_places").document(dreamPlace.getId())
                                .update("photoPaths", updatedPhotoPaths);
                    }
                    if (photoUris.isEmpty()) {
                        Toast.makeText(this, "All photos deleted", Toast.LENGTH_SHORT).show();
                        finish(); // or redirect back
                    }
                }
            }
        }
    }
    // --- MapView lifecycle forwarding (required for embedded Google Maps) ---
    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}
