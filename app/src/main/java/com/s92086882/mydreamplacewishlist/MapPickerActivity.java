package com.s92086882.mydreamplacewishlist;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A simple map picker flow:
 * - User can search by place name (geocoding → move camera + drop marker).
 * - Or tap the map to choose a coordinate (reverse geocoding → label marker).
 * - After a selection, shows a confirmation dialog and returns lat/lng + city + name to caller.
 * -
 * Caller:
 * - Typically AddDreamPlaceActivity via startActivityForResult / Activity Result API.
 * - Expects "lat","lng","city","name" in the result Intent on RESULT_OK.
 */
public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap; // Map instance once ready
    private Marker currentMarker; // The single marker we maintain
    private EditText searchEditText; // Input for geocoder text search
    private Button searchButton;

    // Selected result fields to pass back
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private String selectedPlaceName = "";
    private String selectedCityName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        // Bind views
        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);

        // Obtain the SupportMapFragment and register the callback
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Trigger a geocoding search for the typed place name
        searchButton.setOnClickListener(v -> searchPlace());
    }

    /** Geocode a user-entered place name, move camera, drop marker, ask to confirm. */
    private void searchPlace() {
        String locationName = searchEditText.getText().toString().trim();
        if (locationName.isEmpty()) {
            Toast.makeText(this, "Enter a place to search", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            // Request a single match for the given name
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // Save selection
                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;
                extractAddressDetails(address, locationName); // fill name/city with sensible fallbacks

                // Keep only one marker on the map
                if (currentMarker != null) currentMarker.remove();
                currentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlaceName));
                // Smoothly zoom to the selection
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

                showConfirmationDialog(); // Ask to confirm selection
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Extracts displayable place name and city from an Address, with fallbacks:
     * - Place name: address line or the original search text.
     * - City: locality → sub-admin area → admin area → "Unknown".
     */
    private void extractAddressDetails(Address address, String fallbackName) {
        // Full readable address line or fallback
        selectedPlaceName = address.getAddressLine(0) != null
                ? address.getAddressLine(0)
                : fallbackName;

        // Determine city-level label from available fields
        if (address.getLocality() != null) {
            selectedCityName = address.getLocality();
        } else if (address.getSubAdminArea() != null) {
            selectedCityName = address.getSubAdminArea();
        } else if (address.getAdminArea() != null) {
            selectedCityName = address.getAdminArea();
        } else {
            selectedCityName = "Unknown";
        }
    }

    /** Packs the selected location details into a result Intent and finishes. */
    private void returnResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("lat", selectedLat);
        resultIntent.putExtra("lng", selectedLng);
        resultIntent.putExtra("city", selectedCityName);
        resultIntent.putExtra("name", selectedPlaceName);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    /** Simple confirmation dialog after user picks a point/search result. */
    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Location")
                .setMessage("Do you want to select this location?")
                .setPositiveButton("Yes", (dialog, which) -> returnResult())
                .setNegativeButton("No", null)
                .show();
    }

    /** Map is ready—set defaults and handle tap-to-pick flow. */
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Start centered on Sri Lanka with a comfortable country-level zoom
        LatLng sriLanka = new LatLng(7.8731, 80.7718);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLanka, 7f));
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Allow user to tap anywhere to pick a location
        mMap.setOnMapClickListener(latLng -> {
            // Reset existing marker
            if (currentMarker != null) currentMarker.remove();

            // Save raw coordinates
            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;

            // Reverse geocode for label/city (best-effort with fallbacks)
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    extractAddressDetails(addresses.get(0), "Selected Place");
                } else {
                    selectedPlaceName = "Selected Place";
                    selectedCityName = "Unknown";
                }
            } catch (IOException e) {
                selectedPlaceName = "Selected Place";
                selectedCityName = "Unknown";
                Toast.makeText(this, "Geocoding failed", Toast.LENGTH_SHORT).show();
            }

            // Drop new marker and zoom in
            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(selectedPlaceName));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

            // Ask for confirmation before returning to caller
            showConfirmationDialog();
        });
    }
}