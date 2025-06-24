package com.s92086882.mydreamplacewishlist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * AccountFragment handles the user account screen.
 * Displays user info, provides navigation to settings/help/legal,
 * and allows sign-out for both logged-in and guest users.
 */
public class AccountFragment extends Fragment {

    private TextView userNameText;
    private Button signOutButton;
    private LinearLayout btnSettings, btnHelp, btnTerms;

    public AccountFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Inflate the layout for the Account tab
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Initialize views
        userNameText = view.findViewById(R.id.text_user_name);
        signOutButton = view.findViewById(R.id.signOutButton);
        btnSettings = view.findViewById(R.id.btn_settings);
        btnHelp = view.findViewById(R.id.btn_help);
        btnTerms = view.findViewById(R.id.btn_terms);

        // Check if user is a guest
        SharedPreferences prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("isGuest", true);

        if (isGuest) {
            // Show 'Guest' as name
            userNameText.setText("Guest User");
        } else {
            // Load logged-in user’s name from Firestore
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                FirebaseFirestore.getInstance().collection("users")
                        .document(user.getUid())
                        .get()
                        .addOnSuccessListener(this::displayUserName);
            }
        }

        // Handle Settings click (optional: replace with actual activity)
        btnSettings.setOnClickListener(v -> {
            // You can create SettingsActivity and start it here
            startActivity(new Intent(requireContext(), SettingsActivity.class));
        });

        // Handle Help & Support click
        btnHelp.setOnClickListener(v -> {
            // You can create HelpActivity and start it here
            startActivity(new Intent(requireContext(), HelpActivity.class));
        });

        // Handle Terms & Privacy click
        btnTerms.setOnClickListener(v -> {
            // You can create TermsActivity and start it here
            startActivity(new Intent(requireContext(), TermsActivity.class));
        });

        // Sign Out button logic
        signOutButton.setOnClickListener(v -> {
            // 1. Set isGuest back to true
            prefs.edit().putBoolean("isGuest", true).apply();

            // 2. Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // 3. Sign out from Google if used
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(
                    requireContext(),
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            );
            googleSignInClient.signOut();

            // 4. Redirect to LoginActivity and clear task stack
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Displays the user's first name on the screen
     */
    private void displayUserName(DocumentSnapshot doc) {
        if (doc.exists()) {
            String firstName = doc.getString("firstName");
            userNameText.setText(firstName != null ? firstName : "User");
        }
    }
}