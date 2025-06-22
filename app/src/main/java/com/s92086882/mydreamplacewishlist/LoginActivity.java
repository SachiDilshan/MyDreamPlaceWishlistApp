package com.s92086882.mydreamplacewishlist;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, guestLoginButton;
    private LinearLayout googleLoginButton;
    private TextView signupRedirect;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_GOOGLE_SIGN_IN = 9002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Init Views
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleLoginButton = findViewById(R.id.googleLoginButton);
        guestLoginButton = findViewById(R.id.guestLoginButton);
        signupRedirect = findViewById(R.id.signupRedirectTextView);
        TextView forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);
        TextView signupRedirect = findViewById(R.id.signupRedirectTextView);

        // Styled clickable "Sign up" text only
        String fullText = "Don't have an account? Sign up";
        SpannableString spannable = new SpannableString(fullText);
        int start = fullText.indexOf("Sign up");
        int end = start + "Sign up".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
                finish();
            }
            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(ContextCompat.getColor(LoginActivity.this, R.color.black));
                ds.setFakeBoldText(true);
                ds.setUnderlineText(false);
            }
        };

        spannable.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        signupRedirect.setText(spannable);
        signupRedirect.setMovementMethod(LinkMovementMethod.getInstance());
        signupRedirect.setHighlightColor(Color.TRANSPARENT);

        // Eye icon click listener to toggle password visibility
        passwordEditText.setOnTouchListener(this::visibilityToggle);

        // Password reset
        forgotPasswordTextView.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                emailEditText.setError("Enter your email first");
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Enter a valid email address");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))  // from google-services.json
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Button Click Listeners
        loginButton.setOnClickListener(v -> signInWithEmail());
        googleLoginButton.setOnClickListener(v -> signInWithGoogle());
        guestLoginButton.setOnClickListener(v -> continueAsGuest());
    }

    /**
     * Email/Password Login
     */
    private void signInWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Input validation
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            return;
        }

        // Firebase sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        getSharedPreferences("auth", MODE_PRIVATE).edit().putBoolean("isGuest", false).apply();
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Launch Google Sign-In Intent
     */
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    /**
     * Continue as Guest User
     */
    private void continueAsGuest() {
        getSharedPreferences("auth", MODE_PRIVATE).edit().putBoolean("isGuest", true).apply();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /**
     * Handle Result from Google Sign-In
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
     * Authenticate with Firebase using Google account and save user data
     */
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        getSharedPreferences("auth", MODE_PRIVATE).edit().putBoolean("isGuest", false).apply();
                        FirebaseUser user = mAuth.getCurrentUser();
                        saveGoogleUserToFirestore(user);
                        Toast.makeText(this, "Google Sign-In successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Firebase auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Store Google user basic profile in Firestore if not exists
     */
    private void saveGoogleUserToFirestore(FirebaseUser user) {
        String uid = user.getUid();
        String name = user.getDisplayName();
        String[] names = name != null ? name.split(" ", 2) : new String[]{"User", ""};

        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", names[0]);
        userData.put("lastName", names.length > 1 ? names[1] : "");
        userData.put("email", user.getEmail());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(userData);
    }

    private boolean visibilityToggle(View v, MotionEvent event) {
        final int DRAWABLE_END = 2;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (event.getRawX() >= (passwordEditText.getRight() - passwordEditText.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {
                int inputType = passwordEditText.getInputType();
                boolean isPasswordVisible = (inputType & InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;

                // Toggle input type
                passwordEditText.setInputType(isPasswordVisible ?
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD :
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

                // Set appropriate eye icon
                passwordEditText.setCompoundDrawablesWithIntrinsicBounds(
                        0, 0,
                        isPasswordVisible ? R.drawable.ic_eye_closed : R.drawable.ic_eye_open,
                        0
                );

                // Keep cursor at end
                passwordEditText.setSelection(passwordEditText.getText().length());

                return true;
            }
        }
        return false;
    }
}