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
 * Activity to display a single photo in fullscreen.
 * Supports back gesture and toolbar navigation.
 */
public class PhotoViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        Toolbar toolbar = findViewById(R.id.viewerToolbar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // Set back button manually (NO setSupportActionBar here!)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.getNavigationIcon().setTint(getResources().getColor(android.R.color.white));
        toolbar.setNavigationOnClickListener(v -> finish());

        // Load photo
        ImageView photoView = findViewById(R.id.fullscreenImageView);
        String photoUri = getIntent().getStringExtra("photo_uri");
        if (photoUri != null) {
            Glide.with(this).load(Uri.parse(photoUri)).into(photoView);
        }

        ImageButton deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("delete_photo_uri", photoUri); // pass the URI back
            setResult(RESULT_OK, resultIntent);
            finish(); // close viewer
        });
    }

    // Optional override for back press (gesture or hardware)
    @Override
    public void onBackPressed() {
        super.onBackPressed(); // Enables system back gesture
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_photo_delete, menu);
        return true;
    }

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