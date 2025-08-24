package com.s92086882.mydreamplacewishlist;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Activity to add a new Dream Place.
 * - Collects user input: name, city, notes.
 * - Allows attaching multiple photos (stored locally or uploaded to Firebase).
 * - Lets user pick location manually (via MapPickerActivity) or use current location.
 * - Saves data into SQLite (guest users) or Firestore + Storage (logged-in users).
 */

public class AddDreamPlaceActivity extends AppCompatActivity {

    // Input fields and buttons
    private EditText placeNameEditText, cityEditText, notesEditText;
    private RecyclerView photoRecyclerView;
    private Button addPhotoButton, addPlaceButton;
    private ImageButton locationPickerButton;

    // Stores selected photo URIs (local copies)
    private List<Uri> photoUris = new ArrayList<>();
    private PhotoAdapter photoAdapter;

    // Firebase objects for logged-in user mode
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    // Guest flag and location variables
    private boolean isGuest;
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;

    // Request codes for activity results & permissions
    private static final int PICK_IMAGES_REQUEST = 1001;
    private static final int PLACE_PICKER_REQUEST = 2001;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // enables edge-to-edge layout
        setContentView(R.layout.activity_add_dream_place);

        // Apply insets only to the toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0); // Only top inset for toolbar
            return insets;
        });

        // For Android M+, set light status bar for better visibility on white background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // Firebase instances
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        // Check guest mode flag from SharedPreferences
        isGuest = getSharedPreferences("auth", MODE_PRIVATE).getBoolean("isGuest", true);

        // Location manager for current device location
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Setup methods
        initViews();
        setupToolbar();
        setupPhotoRecyclerView();
        checkLocationPermission();
        setupListeners();
    }

    /** Initialize UI elements */
    private void initViews() {
        placeNameEditText = findViewById(R.id.placeNameEditText);
        cityEditText = findViewById(R.id.cityEditText);
        notesEditText = findViewById(R.id.notesEditText);
        photoRecyclerView = findViewById(R.id.photoRecyclerView);
        addPhotoButton = findViewById(R.id.addPhotoButton);
        addPlaceButton = findViewById(R.id.addPlaceButton);
        locationPickerButton = findViewById(R.id.locationPickerButton);
    }

    /** Configure toolbar with back button and no title */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /** Setup photo list RecyclerView + drag-and-drop reorder */
    private void setupPhotoRecyclerView() {
        photoAdapter = new PhotoAdapter(photoUris);
        photoRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        photoRecyclerView.setAdapter(photoAdapter);

        // Drag-and-drop support with ItemTouchHelper
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, 0) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
                Collections.swap(photoUris, from.getAdapterPosition(), to.getAdapterPosition());
                photoAdapter.notifyItemMoved(from.getAdapterPosition(), to.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {}
        });
        helper.attachToRecyclerView(photoRecyclerView);
    }

    /** Set up listeners for photo add, location picker, and save button */
    private void setupListeners() {
        // Pick multiple images
        addPhotoButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(intent, "Select Pictures"), PICK_IMAGES_REQUEST);
        });

        // Open map picker activity to choose location
        locationPickerButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapPickerActivity.class);
            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        });

        // Save Dream Place (to Firestore or SQLite depending on login state)
        addPlaceButton.setOnClickListener(v -> saveDreamPlace());
    }

    /** Check and request location permission */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchCurrentLocation();
        }
    }

    /** Fetch last known location if available */
    private void fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location != null) {
            selectedLat = location.getLatitude();
            selectedLng = location.getLongitude();
        }
    }

    /** Handle runtime permission result */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Handle photo picker and map picker results */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Multiple or single photo selection
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri sourceUri = data.getClipData().getItemAt(i).getUri();
                    Uri localUri = copyToInternalStorage(sourceUri);
                    if (localUri != null) {
                        photoUris.add(localUri);
                    }
                }
            } else if (data.getData() != null) {
                Uri sourceUri = data.getData();
                Uri localUri = copyToInternalStorage(sourceUri);
                if (localUri != null) {
                    photoUris.add(localUri);
                }
            }
            photoAdapter.notifyDataSetChanged();
        }

        // Location picked from map
        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedLat = data.getDoubleExtra("lat", 0.0);
            selectedLng = data.getDoubleExtra("lng", 0.0);
            String city = data.getStringExtra("city");
            String name = data.getStringExtra("name");

            if (city != null) cityEditText.setText(city);
            if (name != null) placeNameEditText.setText(name);
        }
    }

    /** Copy selected photo from external content URI into app's internal storage */
    private Uri copyToInternalStorage(Uri sourceUri) {
        try {
            String filename = "img_" + System.currentTimeMillis() + ".jpg";
            File file = new File(getFilesDir(), filename);

            try (InputStream in = getContentResolver().openInputStream(sourceUri);
                 FileOutputStream out = new FileOutputStream(file)) {

                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }

            return Uri.fromFile(file);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to copy image", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /** Validate input and decide where to save (SQLite vs Firestore) */
    private void saveDreamPlace() {
        String name = placeNameEditText.getText().toString().trim();
        String city = cityEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(city)) {
            Toast.makeText(this, "Name and City are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isGuest) {
            saveToSQLite(name, city, notes, photoUris);
        } else {
            saveToFirestore(name, city, notes, photoUris);
        }
    }

    /** Save Dream Place into SQLite (guest mode) */
    private void saveToSQLite(String name, String city, String notes, List<Uri> photos) {
        DreamPlaceSQLiteHelper dbHelper = new DreamPlaceSQLiteHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DreamPlaceSQLiteHelper.COLUMN_NAME, name);
        values.put(DreamPlaceSQLiteHelper.COLUMN_CITY, city);
        values.put(DreamPlaceSQLiteHelper.COLUMN_NOTES, notes);
        values.put(DreamPlaceSQLiteHelper.COLUMN_LAT, selectedLat);
        values.put(DreamPlaceSQLiteHelper.COLUMN_LNG, selectedLng);
        values.put(DreamPlaceSQLiteHelper.COLUMN_PHOTOS, joinUris(photos));
        values.put(DreamPlaceSQLiteHelper.COLUMN_VISITED, 0);
        values.put(DreamPlaceSQLiteHelper.COLUMN_RATING, 0f);

        long result = db.insert(DreamPlaceSQLiteHelper.TABLE_NAME, null, values);
        db.close();

        if (result != -1) {
            Toast.makeText(this, "Saved locally (guest)", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
        }
    }

    /** Join multiple photo URIs into one string for SQLite */
    private String joinUris(List<Uri> uris) {
        List<String> strUris = new ArrayList<>();
        for (Uri uri : uris) {
            strUris.add(uri.toString());
        }
        return TextUtils.join(",", strUris);
    }

    /** Save Dream Place into Firestore + upload photos to Firebase Storage */
    private void saveToFirestore(String name, String city, String notes, List<Uri> photos) {
        String uid = mAuth.getCurrentUser().getUid();
        String docId = UUID.randomUUID().toString();

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("city", city);
        data.put("notes", notes);
        data.put("latitude", selectedLat);
        data.put("longitude", selectedLng);
        data.put("visited", false);
        data.put("rating", 0);
        data.put("timestamp", System.currentTimeMillis());

        List<String> photoUrls = new ArrayList<>();

        // If no photos, upload document directly
        if (photos.isEmpty()) {
            data.put("photos", photoUrls);
            uploadPlaceData(uid, docId, data);
            return;
        }

        // Upload each photo asynchronously to Firebase Storage
        for (int i = 0; i < photos.size(); i++) {
            Uri photoUri = photos.get(i);
            String photoName = "photo_" + System.currentTimeMillis() + "_" + i + ".jpg";

            FirebaseStorage.getInstance().getReference("users/" + uid + "/places/" + docId + "/" + photoName)
                    .putFile(photoUri)
                    .addOnSuccessListener(taskSnapshot ->
                            taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                                photoUrls.add(uri.toString());
                                if (photoUrls.size() == photos.size()) {
                                    data.put("photos", photoUrls);
                                    uploadPlaceData(uid, docId, data);
                                }
                            }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Photo upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        }
    }

    /** Final upload of Dream Place document into Firestore */
    private void uploadPlaceData(String uid, String docId, Map<String, Object> data) {
        firestore.collection("users").document(uid)
                .collection("dream_places").document(docId)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Dream place added", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
