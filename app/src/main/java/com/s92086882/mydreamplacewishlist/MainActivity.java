package com.s92086882.mydreamplacewishlist;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;

/**
 * App entry activity that hosts the bottom navigation and swaps fragments.
 * -
 * Responsibilities:
 * - Initializes Firebase (FirebaseApp.initializeApp).
 * - Hosts a single fragment container (R.id.nav_host_fragment).
 * - Loads HomeFragment on first launch.
 * - Handles bottom navigation item selection to replace the active fragment.
 * -
 * Notes:
 * - Uses .replace(...) without adding to back stack, so pressing the system back button
 *   will exit the app from the root tab instead of navigating through previous tabs.
 * - savedInstanceState guard ensures we don't recreate the default fragment on configuration changes.
 */
public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this); // Ensure Firebase SDK is initialized before any Firebase use
        setContentView(R.layout.activity_main); // Layout contains BottomNavigationView + fragment container

        bottomNav = findViewById(R.id.bottom_navigation);

        // Load default fragment only on a fresh start (avoid duplicating fragments on rotate/recreate)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_home); // visually select Home tab
        }

        setupNavigation(); // wire bottom nav selection handling
    }

    /**
     * Sets up the BottomNavigationView to swap fragments when tabs are selected.
     * - Creates a new fragment each time a tab is selected and replaces the container content.
     * - No back stack entry is added (simple tab behavior).
     */
    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Map each menu item to its corresponding Fragment
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_map) {
                selectedFragment = new MapFragment();
            } else if (itemId == R.id.nav_search) {
                selectedFragment = new SearchFragment();
            } else if (itemId == R.id.nav_account) {
                selectedFragment = new AccountFragment();
            }

            // Replace current fragment if a valid tab was chosen
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
                return true;
            }
            return false; // not handled
        });
    }
}
