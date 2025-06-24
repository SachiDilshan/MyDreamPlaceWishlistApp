package com.s92086882.mydreamplacewishlist;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for searching Dream Places by name or city.
 * Supports both guest (SQLite) and logged-in (Firestore) users.
 * Calculates distance if location permission is granted.
 */
public class SearchFragment extends Fragment {

    private EditText searchEditText;
    private RecyclerView resultsRecyclerView;
    private DreamPlaceAdapter adapter;

    // Full list of dream places (from DB or Firestore)
    private final List<DreamPlace> allPlaces = new ArrayList<>();
    // Filtered results for search display
    private final List<DreamPlace> filteredPlaces = new ArrayList<>();

    private boolean isGuest;
    private Location userLocation;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Initialize views
        searchEditText = view.findViewById(R.id.searchEditText);
        resultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);

        // Setup RecyclerView
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new DreamPlaceAdapter(requireContext(), filteredPlaces, place -> {
            // When a place is clicked, open it in MyDreamPlaceActivity
            Intent intent = new Intent(requireContext(), MyDreamPlaceActivity.class);
            intent.putExtra("dream_place", place); // DreamPlace must implement Serializable
            startActivity(intent);
        });
        resultsRecyclerView.setAdapter(adapter);

        // Check if user is a guest
        isGuest = SharedPreferencesHelper.isGuest(requireContext());

        // Request location permission if not already granted
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getUserLocation(); // Permission granted → fetch location
        } else {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        // Add live filtering as the user types
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPlaces(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    /**
     * Safely fetch user's last known location, then load places.
     */
    private void getUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Safety net: permission not granted — don’t proceed
            Toast.makeText(getContext(), "Location permission missing", Toast.LENGTH_SHORT).show();
            loadAllPlaces(); // fallback without location
            return;
        }

        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        locationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLocation = location;
                        loadAllPlaces(); // Load only after getting location
                    } else {
                        Toast.makeText(getContext(), "Unable to get location", Toast.LENGTH_SHORT).show();
                        loadAllPlaces(); // fallback
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Location failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    loadAllPlaces();
                });
    }

    /**
     * Loads all dream places based on user type.
     * Guests → local SQLite DB, Logged-in users → Firebase Firestore.
     * Also calculates distance if userLocation is available.
     */
    private void loadAllPlaces() {
        if (isGuest) {
            // Load places from SQLite for guest users
            DreamPlaceSQLiteHelper db = new DreamPlaceSQLiteHelper(requireContext());
            allPlaces.clear();
            allPlaces.addAll(db.getAllDreamPlaces());

            calculateDistanceForPlaces();
            filteredPlaces.clear();
            filteredPlaces.addAll(allPlaces);
            adapter.notifyDataSetChanged();
        } else {
            // Load places from Firestore for logged-in users
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .collection("dream_places")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        allPlaces.clear();
                        for (DocumentSnapshot doc : snapshot) {
                            DreamPlace place = doc.toObject(DreamPlace.class);
                            if (place != null) allPlaces.add(place);
                        }

                        calculateDistanceForPlaces();
                        filteredPlaces.clear();
                        filteredPlaces.addAll(allPlaces);
                        adapter.notifyDataSetChanged();
                    });
        }
        calculateDistanceForPlaces();

        // Sort by distance if available
        if (userLocation != null) {
            allPlaces.sort((p1, p2) -> {
                float d1 = parseDistance(p1.getDistance());
                float d2 = parseDistance(p2.getDistance());
                return Float.compare(d1, d2); // ascending
            });
        }

        filteredPlaces.clear();
        filteredPlaces.addAll(allPlaces);
        adapter.notifyDataSetChanged();
    }
    private float parseDistance(String distanceText) {
        if (distanceText == null) return Float.MAX_VALUE;

        try {
            if (distanceText.contains("km")) {
                return Float.parseFloat(distanceText.replace(" km away", "")) * 1000;
            } else if (distanceText.contains("m")) {
                return Float.parseFloat(distanceText.replace(" m away", ""));
            }
        } catch (NumberFormatException e) {
            return Float.MAX_VALUE;
        }
        return Float.MAX_VALUE;
    }

    /**
     * Filters the dream place list based on the query (name or city).
     * @param query The user's typed search input
     */
    private void filterPlaces(String query) {
        filteredPlaces.clear();
        String lowerQuery = query.toLowerCase();

        for (DreamPlace place : allPlaces) {
            if (place.getName().toLowerCase().contains(lowerQuery) ||
                    place.getCity().toLowerCase().contains(lowerQuery)) {
                filteredPlaces.add(place);
            }
        }
        adapter.notifyDataSetChanged();
    }

    /**
     * Calculates the distance between userLocation and each place,
     * and sets the formatted distance string in each DreamPlace object.
     */
    private void calculateDistanceForPlaces() {
        if (userLocation == null) return;

        for (DreamPlace place : allPlaces) {
            if (place.getLatitude() != 0 && place.getLongitude() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(
                        userLocation.getLatitude(), userLocation.getLongitude(),
                        place.getLatitude(), place.getLongitude(),
                        results
                );
                float distanceInMeters = results[0];
                String distanceText = distanceInMeters < 1000
                        ? String.format("%.0f m away", distanceInMeters)
                        : String.format("%.2f km away", distanceInMeters / 1000);
                place.setDistance(distanceText);
            }
        }
    }

    /**
     * Handles the result of location permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation(); // Safe to proceed
        } else {
            Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
            loadAllPlaces(); // Fallback without location
        }
    }
}