package com.s92086882.mydreamplacewishlist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * HomeFragment displays the main list of Dream Places.
 *
 * Responsibilities:
 * - Shows greeting with time + user name (if logged in).
 * - Lists Dream Places in a RecyclerView, ordered by distance.
 * - Supports swipe-to-delete with Undo.
 * - Provides FAB to add new places.
 * - Handles guest vs logged-in user data (SQLite vs Firestore).
 * - Fetches live location for distance calculation.
 */
public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private DreamPlaceAdapter adapter;
    private final List<DreamPlace> dreamPlaces = new ArrayList<>();
    private boolean isGuest;

    // Current location state
    private double currentLat = 0.0, currentLng = 0.0;
    private boolean locationLoaded = false;
    private ProgressBar progressBar;

    private FusedLocationProviderClient fusedLocationClient;

    // Launcher for opening MyDreamPlaceActivity and refreshing when returning
    private final ActivityResultLauncher<Intent> dreamPlaceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> loadData()
    );

    // Request permission launcher for location
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) fetchLiveLocation();
                else {
                    Toast.makeText(getContext(), "Location permission is required to sort places by distance", Toast.LENGTH_LONG).show();
                    loadData();
                }
            });

    public HomeFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Force light status bar text for better visibility on white background
        requireActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.bringToFront(); // ensures it overlays RecyclerView

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Greeting section
        TextView greetingText = view.findViewById(R.id.greetingText);
        ImageView avatarIcon = view.findViewById(R.id.avatarIcon);
        String greetingTime = getGreetingMessage();

        // Detect login type from SharedPreferences
        SharedPreferences prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
        isGuest = prefs.getBoolean("isGuest", true);

        // Greeting message differs for guest vs logged-in
        if (isGuest) greetingText.setText(getString(R.string.guest_greeting, greetingTime));
        else {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(doc -> {
                            String firstName = doc.getString("firstName");
                            if (firstName != null && !firstName.isEmpty())
                                greetingText.setText(getString(R.string.user_greeting, firstName, greetingTime));
                            else greetingText.setText(greetingTime);
                        })
                        .addOnFailureListener(e -> greetingText.setText(greetingTime));
            } else greetingText.setText(greetingTime);
        }

        // Avatar click redirects guest to login
        avatarIcon.setOnClickListener(v -> {
            if (isGuest) startActivity(new Intent(getContext(), LoginActivity.class));
        });

        // RecyclerView setup for Dream Places
        recyclerView = view.findViewById(R.id.recyclerViewDreamPlaces);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DreamPlaceAdapter(requireContext(), dreamPlaces, place -> {
            Intent intent = new Intent(getContext(), MyDreamPlaceActivity.class);
            intent.putExtra("dreamPlace", place); // pass model via Serializable
            dreamPlaceLauncher.launch(intent);
        });
        recyclerView.setAdapter(adapter);

        // Enable swipe-to-delete gesture
        setupSwipeToDelete();

        // Launcher for AddDreamPlaceActivity
        final ActivityResultLauncher<Intent> addPlaceLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Reload Home data when user returns from AddDreamPlace
                    loadData();
                }
        );

        // Floating Action Button for adding new places
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddDreamPlaceActivity.class);
            addPlaceLauncher.launch(intent);
        });

        checkLocationPermission();

        return view;
    }

    /** Configure swipe-to-delete with custom background, icon, and Undo option. */
    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                DreamPlace deletedPlace = dreamPlaces.get(position);

                // Remove from RecyclerView and notify
                dreamPlaces.remove(position);
                adapter.notifyItemRemoved(position);

                // Show Snackbar with Undo option
                Snackbar.make(recyclerView, "Place deleted", Snackbar.LENGTH_LONG)
                        .setAction("Undo", v -> {
                            // Restore on undo
                            dreamPlaces.add(position, deletedPlace);
                            adapter.notifyItemInserted(position);
                        })
                        .addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                // If not undone, delete permanently
                                if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                    if (isGuest) {
                                        new DreamPlaceSQLiteHelper(requireContext())
                                                .deleteDreamPlaceByName(deletedPlace.getName());
                                    } else {
                                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                        if (user != null) {
                                            FirebaseFirestore.getInstance()
                                                    .collection("users")
                                                    .document(user.getUid())
                                                    .collection("dream_places")
                                                    .document(deletedPlace.getId())
                                                    .delete();
                                        }
                                    }
                                }
                            }
                        }).show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {
                // Red background + delete icon + text during swipe
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addActionIcon(R.drawable.ic_delete_red)
                        .addSwipeLeftLabel("<- Delete")
                        .setSwipeLeftLabelColor(getResources().getColor(android.R.color.holo_red_dark))
                        .create()
                        .decorate();
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(recyclerView);
    }


    /** Check location permission and request if missing. */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) fetchLiveLocation();
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /** Fetch live location using FusedLocationProviderClient. */
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
            } else Toast.makeText(getContext(), "Unable to fetch current location", Toast.LENGTH_SHORT).show();
            loadData();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            loadData();
        });
    }

    // Load dream place data from Firestore or SQLite based on user type
    private void loadData() {
        if (!isAdded()) return;
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        } // Show progress while loading
        if (isGuest) loadFromSQLite();
        else loadFromFirestore();
    }

    /** Fetch data from Firestore for logged-in users. */
    private void loadFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            progressBar.setVisibility(View.GONE); // Stop if no user
            return;
        }
        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .collection("dream_places")
                .get()
                .addOnSuccessListener(this::handleFirestoreResults)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load places", Toast.LENGTH_SHORT).show());
    }

    /** Fetch data from SQLite for guest users. */
    private void loadFromSQLite() {
        Context context = getContext();
        if (context == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        DreamPlaceSQLiteHelper dbHelper = new DreamPlaceSQLiteHelper(requireContext());
        dreamPlaces.clear();
        dreamPlaces.addAll(dbHelper.getAllPlacesOrderedByDistance(currentLat, currentLng));
        sortAndUpdateRecycler();
        progressBar.setVisibility(View.GONE); // Hide after sorting
    }

    /** Convert Firestore docs into DreamPlace objects and update RecyclerView. */
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
            place.setId(doc.getId()); // Firestore document ID
            dreamPlaces.add(place);
        }
        sortAndUpdateRecycler();
        progressBar.setVisibility(View.GONE); // Hide after sorting
    }

    /** Sort places by numeric distance value and refresh RecyclerView. */
    private void sortAndUpdateRecycler() {
        Collections.sort(dreamPlaces, Comparator.comparing(dp -> {
            try {
                return Float.parseFloat(dp.getDistance().replace(" km away", ""));
            } catch (Exception e) {
                return 9999f; // fallback for missing/invalid distance
            }
        }));
        adapter.updateList(dreamPlaces);
    }

    /** Get greeting based on time of day. */
    private String getGreetingMessage() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 12) return "Good Morning!";
        else if (hour >= 12 && hour < 17) return "Good Afternoon!";
        else if (hour >= 17 && hour < 21) return "Good Evening!";
        else return "Good Night!";
    }

    /** Utility: calculate distance in km between two lat/lng pairs. */
    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        if (lat1 == 0.0 && lng1 == 0.0) return 9999.0;
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000.0;
    }
}
