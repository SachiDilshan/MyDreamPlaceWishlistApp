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
 * Simple "Help & Support" screen.
 * - Uses a custom toolbar (back arrow + centered title).
 * - Applies edge-to-edge window insets so content doesn't clash with system bars.
 * - Finishes activity when back arrow is tapped.
 * -
 * Notes:
 * - Title text is set programmatically to keep layout reusable.
 * - Make sure activity_help.xml has views with ids: toolbar, back_button, header_title.
 */
public class HelpActivity extends AppCompatActivity {

    // Toolbar UI
    private ImageView backButton;
    private TextView headerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this); // Enables drawing behind system bars for modern look
        setContentView(R.layout.activity_help); // Inflate the Help screen layout

        // Apply system bar insets to the custom toolbar only.
        // This ensures the toolbar is padded below the status bar on devices with cutouts/notches.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews(); // Bind views and set static title
        setupToolbar(); // Wire the back arrow behavior
    }

    /** Binds views from layout and sets the screen header text. */
    private void initViews() {
        backButton = findViewById(R.id.back_button); // Left back icon in custom toolbar
        headerTitle = findViewById(R.id.header_title); // Centered title TextView
        headerTitle.setText("Help & Support");  // Set page title
    }

    /** Hooks up toolbar interactions (back button -> close this activity). */
    private void setupToolbar() {
        backButton.setOnClickListener(v -> finish()); // Close and return to previous screen
    }
}