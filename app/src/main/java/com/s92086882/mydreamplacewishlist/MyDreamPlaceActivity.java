package com.s92086882.mydreamplacewishlist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

public class MyDreamPlaceActivity extends AppCompatActivity {

    private EditText nameEditText, cityEditText, notesEditText;
    private RecyclerView photoRecyclerView;
    private CheckBox visitedCheckBox;
    private RatingBar ratingBar;
    private Button saveButton;

    private PhotoAdapter photoAdapter;
    private DreamPlace dreamPlace;
    private boolean isGuest;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private DreamPlaceSQLiteHelper dbHelper;

    private MapView mapView;
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_dream_place);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        dbHelper = new DreamPlaceSQLiteHelper(this);
        isGuest = getSharedPreferences("auth", MODE_PRIVATE).getBoolean("isGuest", true);

        initViews();
        setupToolbar();
        loadPlaceDataFromIntent();
        setupListeners();
    }

    private void initViews() {
        nameEditText = findViewById(R.id.editTextName);
        cityEditText = findViewById(R.id.editTextCity);
        notesEditText = findViewById(R.id.editTextNotes);
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        visitedCheckBox = findViewById(R.id.checkboxVisited);
        ratingBar = findViewById(R.id.ratingBar);
        saveButton = findViewById(R.id.saveButton);

        photoRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(null);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_share) {
                sharePlace();
                return true;
            }
            return false;
        });
    }

    private void loadPlaceDataFromIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("dreamPlace")) {
            dreamPlace = (DreamPlace) intent.getSerializableExtra("dreamPlace");

            if (dreamPlace != null) {
                nameEditText.setText(dreamPlace.getName());
                cityEditText.setText(dreamPlace.getCity());
                notesEditText.setText(dreamPlace.getNotes());
                visitedCheckBox.setChecked(dreamPlace.isVisited());
                ratingBar.setRating(dreamPlace.getRating());
                ratingBar.setVisibility(dreamPlace.isVisited() ? View.VISIBLE : View.GONE);

                List<Uri> photoUris = new ArrayList<>();
                for (String uriStr : dreamPlace.getPhotoPaths()) {
                    photoUris.add(Uri.parse(uriStr));
                }
                photoAdapter = new PhotoAdapter(photoUris);
                photoRecyclerView.setAdapter(photoAdapter);

                mapView.getMapAsync(map -> {
                    googleMap = map;
                    MapsInitializer.initialize(this);
                    LatLng location = new LatLng(dreamPlace.getLatitude(), dreamPlace.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(location).title(dreamPlace.getName()));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
                });
            }
        }
    }

    private void setupListeners() {
        visitedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ratingBar.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        saveButton.setOnClickListener(v -> saveUpdatedPlace());
    }

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

        dreamPlace.setName(name);
        dreamPlace.setCity(city);
        dreamPlace.setNotes(notes);
        dreamPlace.setVisited(visited);
        dreamPlace.setRating(rating);

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

            Map<String, Object> updated = new HashMap<>();
            updated.put("name", name);
            updated.put("city", city);
            updated.put("notes", notes);
            updated.put("visited", visited);
            updated.put("rating", rating);

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

    private void sharePlace() {
        if (dreamPlace == null) return;

        String text = "Check out this place!\n" +
                "Name: " + dreamPlace.getName() + "\n" +
                "City: " + dreamPlace.getCity() + "\n" +
                "Notes: " + dreamPlace.getNotes();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

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
