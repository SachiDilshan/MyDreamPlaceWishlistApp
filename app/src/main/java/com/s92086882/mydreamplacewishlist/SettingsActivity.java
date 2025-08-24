package com.s92086882.mydreamplacewishlist;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Simple Settings screen scaffold.
 * -
 * Responsibilities:
 * - Displays a custom toolbar with a back arrow and title.
 * - Applies edge-to-edge window insets so the toolbar avoids the status bar/cutouts.
 * - Provides a hook (setupListeners) to add actual settings interactions later.
 */
public class SettingsActivity extends AppCompatActivity {

    private ImageView backButton; // left arrow icon in the toolbar
    private TextView headerTitle; // centered title text

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        // Apply system bar insets only to the header container
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews(); // bind views and set title
        setupToolbar(); // wire back navigation
        setupListeners(); // optional: for more interactivity
    }

    /** Bind UI references from layout and set the header title. */
    private void initViews() {
        backButton = findViewById(R.id.back_button);
        headerTitle = findViewById(R.id.header_title);
        headerTitle.setText("Settings");
    }

    private void setupToolbar() {
        // Back button navigates back
        backButton.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // Add more interactions here
    }
}