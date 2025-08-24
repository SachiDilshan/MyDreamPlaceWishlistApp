package com.s92086882.mydreamplacewishlist;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * SignupActivity handles user registration.
 * -
 * Features:
 * - Register using Email/Password with validation rules.
 * - Google Sign-In (OAuth) integration with Firebase.
 * - Saves user profile info (first/last name, email) to Firestore.
 * - Toggles password visibility with an eye icon.
 * - Redirects to LoginActivity or MainActivity after signup.
 * -
 * SharedPreferences:
 * - Updates "isGuest" = false after successful signup.
 */
public class SignupActivity extends AppCompatActivity {

    // UI Components
    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private Button signupButton, loginRedirectButton;
    private LinearLayout googleSignupButton;

    // Firebase components
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    // Password toggle flag
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Handle system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signupButton = findViewById(R.id.signupButton);
        loginRedirectButton = findViewById(R.id.loginRedirectButton);
        googleSignupButton = findViewById(R.id.googleSignupButton);

        // Eye icon click listener to toggle password visibility
        passwordEditText.setOnTouchListener(this::visibilityToggle);

        // Button listeners
        signupButton.setOnClickListener(v -> createAccountWithEmail());
        loginRedirectButton.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        googleSignupButton.setOnClickListener(v -> signInWithGoogle());

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Found in google-services.json
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    /**
     * Create new Firebase user with email/password.
     * Validates all fields before signup and stores user profile in Firestore.
     */
    private void createAccountWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate First Name
        if (TextUtils.isEmpty(firstNameEditText.getText().toString().trim())) {
            firstNameEditText.setError("First name is required");
            return;
        }

        // Validate email: Email required and correct format
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return;
        }

        // Validate password
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError("At least 6 characters required");
            return;
        }
        if (!password.matches(".*\\d.*")) {
            passwordEditText.setError("Must include at least one number");
            return;
        }

        // Create Firebase user
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Automatically logged in, so update guest status
                        getSharedPreferences("auth", MODE_PRIVATE)
                                .edit()
                                .putBoolean("isGuest", false)
                                .apply();

                        // Save user data to Firestore
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserProfileToFirestore(user);

                        Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Launches the Google sign-in intent
     */
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    /**
     * Callback for Google Sign-In result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Authenticates with Firebase using Google credentials
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveUserProfileToFirestore(user); // Save even on Google sign-in
                        Toast.makeText(this, "Google Sign-In successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Firebase auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Save first name, last name and email to Firestore under users collection
     */
    private void saveUserProfileToFirestore(FirebaseUser user) {
        String uid = user.getUid();
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();

        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        userData.put("email", user.getEmail());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "User profile saved", Toast.LENGTH_SHORT).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    /**
     * Toggles password visibility when user taps eye icon in EditText.
     */
    private boolean visibilityToggle(View v, MotionEvent event) {
        final int DRAWABLE_END = 2;  // index for right compound drawable
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (event.getRawX() >= (passwordEditText.getRight()
                    - passwordEditText.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {

                // Flip visibility state
                isPasswordVisible = !isPasswordVisible;
                passwordEditText.setInputType(isPasswordVisible ?
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                // Update icon accordingly
                passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0,
                        isPasswordVisible ? R.drawable.ic_eye_open : R.drawable.ic_eye_closed,
                        0
                );
                passwordEditText.setSelection(passwordEditText.getText().length());
                return true;
            }
        }
        return false;
    }
}