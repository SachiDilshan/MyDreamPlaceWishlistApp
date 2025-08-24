package com.s92086882.mydreamplacewishlist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

/**
 * Full-screen photo viewer.
 * -
 * Responsibilities:
 * - Displays a single image (URI passed via Intent "photo_uri").
 * - Provides a top toolbar with manual back navigation.
 * - Offers two delete affordances:
 *      1) Floating/Image button -> returns "delete_photo_uri" (used by caller to delete a specific photo).
 *      2) Toolbar menu action -> returns "delete_requested" (boolean flag; caller decides how to handle).
 * -
 * Notes:
 * - The layout uses an ImageView with id fullscreenImageView.
 *   PhotoView dependency is imported but not used here (ok if your XML uses a plain ImageView).
 * - Insets are applied to the toolbar to avoid status bar overlap on edge-to-edge screens.
 */
public class PhotoViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        Toolbar toolbar = findViewById(R.id.viewerToolbar);

        // Apply system bar insets to the toolbar so it doesn't sit under the status bar.
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Set back button manually (NO setSupportActionBar here!)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        toolbar.setNavigationOnClickListener(v -> finish());

        // Load the image into the full-screen ImageView using Glide.
        ImageView photoView = findViewById(R.id.fullscreenImageView);
        String photoUri = getIntent().getStringExtra("photo_uri");
        if (photoUri != null) {
            Glide.with(this).load(Uri.parse(photoUri)).into(photoView);
        }

        // Inline delete button:
        // Returns the concrete URI back to caller so it can remove exactly this image.
        ImageButton deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("delete_photo_uri", photoUri); // specific photo to delete
            setResult(RESULT_OK, resultIntent);
            finish(); // close viewer
        });
    }

    // Optional override for back press (gesture or hardware)
    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Enables system back gesture
    }

    /** Inflate toolbar menu (contains a delete action as an alternative affordance). */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_delete, menu);
        return true;
    }

    /**
     * Toolbar menu action:
     * - Returns only a "delete_requested" flag (no URI).
     * - Caller must decide how to process this (e.g., delete current image in its adapter context).
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_delete) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("delete_requested", true);
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}