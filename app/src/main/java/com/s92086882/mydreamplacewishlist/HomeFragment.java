package com.s92086882.mydreamplacewishlist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private DreamPlaceAdapter adapter;
    private final List<DreamPlace> dreamPlaces = new ArrayList<>();
    private boolean isGuest;

    private double currentLat = 0.0, currentLng = 0.0;
    private boolean locationLoaded = false;

    private FusedLocationProviderClient fusedLocationClient;

    // Request location permission result handler
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    fetchLiveLocation(); // If granted, fetch location
                } else {
                    Toast.makeText(getContext(), "Location permission is required to sort places by distance", Toast.LENGTH_LONG).show();
                    loadData(); // Still load data without sorting
                }
            });

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Set status bar text to dark on light backgrounds
        requireActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // Initialize fused location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Greeting logic
        TextView greetingText = view.findViewById(R.id.greetingText);
        ImageView avatarIcon = view.findViewById(R.id.avatarIcon);
        String greetingTime = getGreetingMessage();

        // Determine if user is guest
        SharedPreferences prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
        isGuest = prefs.getBoolean("isGuest", true);

        if (isGuest) {
            greetingText.setText(getString(R.string.guest_greeting, greetingTime));
        } else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            String firstName = doc.getString("firstName");
                            if (firstName != null && !firstName.isEmpty()) {
                                greetingText.setText(getString(R.string.user_greeting, firstName, greetingTime));
                            } else {
                                greetingText.setText(greetingTime);
                            }
                        })
                        .addOnFailureListener(e -> greetingText.setText(greetingTime));
            } else {
                greetingText.setText(greetingTime);
            }
        }

        // Avatar click to login (for guest only)
        avatarIcon.setOnClickListener(v -> {
            if (isGuest) {
                startActivity(new Intent(getContext(), LoginActivity.class));
            }
        });

        // Setup RecyclerView and Adapter
        recyclerView = view.findViewById(R.id.recyclerViewDreamPlaces);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DreamPlaceAdapter(requireContext(), dreamPlaces, place -> {
            Intent intent = new Intent(getContext(), MyDreamPlaceActivity.class);
            intent.putExtra("dreamPlace", place); // Passing full DreamPlace
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        // Floating Action Button to add new Dream Place
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> startActivity(new Intent(getContext(), AddDreamPlaceActivity.class)));

        checkLocationPermission(); // Start location permission check

        return view;
    }

    // Checks if location permission is granted, otherwise asks for it
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLiveLocation();
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    // Fetch user’s current location using FusedLocationProviderClient
    private void fetchLiveLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
            if (location != null) {
                currentLat = location.getLatitude();
                currentLng = location.getLongitude();
                locationLoaded = true;
                Log.d("HomeFragment", "Lat: " + currentLat + ", Lng: " + currentLng);
            } else {
                Toast.makeText(getContext(), "Unable to fetch current location", Toast.LENGTH_SHORT).show();
            }
            loadData();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadData();
        });
    }

    // Loads DreamPlaces based on login status
    private void loadData() {
        if (!isAdded()) return;
        if (isGuest) {
            loadFromSQLite();
        } else {
            loadFromFirestore();
        }
    }

    // For logged-in users – Load DreamPlaces from Firestore
    private void loadFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .collection("dream_places")
                .get()
                .addOnSuccessListener(this::handleFirestoreResults)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load places", Toast.LENGTH_SHORT).show());
    }

    // Parse Firestore documents into DreamPlace objects
    private void handleFirestoreResults(QuerySnapshot querySnapshot) {
        dreamPlaces.clear();
        for (QueryDocumentSnapshot doc : querySnapshot) {
            String name = doc.getString("name");
            String city = doc.getString("city");
            String notes = doc.getString("notes");
            Double lat = doc.getDouble("latitude");
            Double lng = doc.getDouble("longitude");
            Boolean visited = doc.getBoolean("visited");
            Double ratingRaw = doc.getDouble("rating");
            List<String> photoUrls = (List<String>) doc.get("photos");

            if (lat == null || lng == null || name == null || city == null || photoUrls == null) continue;

            float rating = ratingRaw != null ? ratingRaw.floatValue() : 0f;
            boolean isVisited = visited != null && visited;

            double distance = calculateDistance(currentLat, currentLng, lat, lng);
            String distanceStr = String.format("%.1f km away", distance);

            DreamPlace place = new DreamPlace(photoUrls, name, city, distanceStr, isVisited, rating, lat, lng, notes);
            place.setId(doc.getId());
            dreamPlaces.add(place);
        }

        sortAndUpdateRecycler();
    }

    // For guest users – Load DreamPlaces from SQLite
    private void loadFromSQLite() {
        Context context = getContext();
        if (context == null) return;

        DreamPlaceSQLiteHelper dbHelper = new DreamPlaceSQLiteHelper(requireContext());
        dreamPlaces.clear();
        dreamPlaces.addAll(dbHelper.getAllPlacesOrderedByDistance(currentLat, currentLng));
        sortAndUpdateRecycler();
    }

    // Sort list by calculated distance and update RecyclerView
    private void sortAndUpdateRecycler() {
        Collections.sort(dreamPlaces, Comparator.comparing(dp -> {
            try {
                return Float.parseFloat(dp.getDistance().replace(" km away", ""));
            } catch (Exception e) {
                return 9999f;
            }
        }));
        adapter.updateList(dreamPlaces);
    }

    // Generate time-based greeting
    private String getGreetingMessage() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Good Morning!";
        else if (hour >= 12 && hour < 17) return "Good Afternoon!";
        else if (hour >= 17 && hour < 21) return "Good Evening!";
        else return "Good Night!";
    }

    // Calculates distance between two lat-lng points (in km)
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        if (lat1 == 0.0 && lng1 == 0.0) return 9999.0;

        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000.0;
    }
}