package com.s92086882.mydreamplacewishlist;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class TermsActivity extends AppCompatActivity {

    private ImageView backButton;
    private TextView headerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terms);

        // Apply system bar insets only to the toolbar (your FrameLayout)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupToolbar();
    }

    private void initViews() {
        backButton = findViewById(R.id.back_button);
        headerTitle = findViewById(R.id.header_title);
        headerTitle.setText("Terms & Privacy");
    }

    private void setupToolbar() {
        backButton.setOnClickListener(v -> finish());
    }
}