package com.s92086882.mydreamplacewishlist;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isGuest;

    public MapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Get the SupportMapFragment and set the map callback
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Check if user is logged in or a guest
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        isGuest = (user == null);

        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Try to enable location and zoom to user location
        enableLocationAndZoom();

        // Move "My Location" button below action bar
        View locationButton = ((View) getView().findViewById(Integer.parseInt("1")).getParent())
                .findViewById(Integer.parseInt("2"));

        ViewGroup.MarginLayoutParams layoutParams =
                (ViewGroup.MarginLayoutParams) locationButton.getLayoutParams();
        layoutParams.setMargins(0, 200, 30, 0); // Adjust top margin as needed
        locationButton.setLayoutParams(layoutParams);

        // Load dream place markers
        if (isGuest) {
            loadFromSQLite();
        } else {
            loadFromFirestore();
        }
    }

    @SuppressLint("MissingPermission")
    private void enableLocationAndZoom() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted – location won't be enabled
            return;
        }

        mMap.setMyLocationEnabled(true);

        // Zoom into current location
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 14f));
                    }
                });
    }

    // For Firebase users: load dream places from Firestore
    private void loadFromFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .collection("dream_places")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        Double lat = doc.getDouble("latitude");
                        Double lng = doc.getDouble("longitude");
                        String name = doc.getString("name");
                        Boolean visited = doc.getBoolean("visited");

                        if (lat != null && lng != null) {
                            LatLng position = new LatLng(lat, lng);
                            int iconRes = (visited != null && visited)
                                    ? R.drawable.marker_visited
                                    : R.drawable.marker_not_visited;
                            mMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.fromResource(iconRes)));
                        }
                    }
                });
    }

    // For guest users: load dream places from local SQLite DB
    private void loadFromSQLite() {
        DreamPlaceSQLiteHelper dbHelper = new DreamPlaceSQLiteHelper(requireContext());
        List<DreamPlace> places = dbHelper.getAllDreamPlaces();

        for (DreamPlace place : places) {
            LatLng position = new LatLng(place.getLatitude(), place.getLongitude());
            int iconRes = place.isVisited()
                    ? R.drawable.marker_visited
                    : R.drawable.marker_not_visited;
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(place.getName())
                    .icon(BitmapDescriptorFactory.fromResource(iconRes)));
        }
    }
}