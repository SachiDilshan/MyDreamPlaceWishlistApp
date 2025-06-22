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

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker currentMarker;
    private EditText searchEditText;
    private Button searchButton;

    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private String selectedPlaceName = "";
    private String selectedCityName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        searchButton.setOnClickListener(v -> searchPlace());
    }

    private void searchPlace() {
        String locationName = searchEditText.getText().toString().trim();
        if (locationName.isEmpty()) {
            Toast.makeText(this, "Enter a place to search", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(locationName, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                selectedLat = latLng.latitude;
                selectedLng = latLng.longitude;
                extractAddressDetails(address, locationName);

                if (currentMarker != null) currentMarker.remove();
                currentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlaceName));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

                showConfirmationDialog(); // Ask to confirm selection
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, "Error fetching location", Toast.LENGTH_SHORT).show();
        }
    }

    private void extractAddressDetails(Address address, String fallbackName) {
        // Full readable address line or fallback
        selectedPlaceName = address.getAddressLine(0) != null
                ? address.getAddressLine(0)
                : fallbackName;

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

    private void returnResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("lat", selectedLat);
        resultIntent.putExtra("lng", selectedLng);
        resultIntent.putExtra("city", selectedCityName);
        resultIntent.putExtra("name", selectedPlaceName);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Location")
                .setMessage("Do you want to select this location?")
                .setPositiveButton("Yes", (dialog, which) -> returnResult())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng sriLanka = new LatLng(7.8731, 80.7718);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLanka, 7f));
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnMapClickListener(latLng -> {
            if (currentMarker != null) currentMarker.remove();

            selectedLat = latLng.latitude;
            selectedLng = latLng.longitude;

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

            currentMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(selectedPlaceName));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

            showConfirmationDialog();
        });
    }
}