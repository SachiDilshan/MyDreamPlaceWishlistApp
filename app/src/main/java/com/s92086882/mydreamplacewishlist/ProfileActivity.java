package com.s92086882.mydreamplacewishlist;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * Your Profile screen:
 * - Loads name/email from Firestore/Auth
 * - Lets user update name, email, password
 * - Lets user change avatar (uploads to Firebase Storage and saves URL in Firestore)
 * - Lets user delete their account (doc + avatar + auth user)
 * -
 * Notes:
 * - Updating email/password may require recent login -> handle exceptions in UI.
 * - For guests, I simply block this page.
 */
public class ProfileActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ShapeableImageView imageAvatar;
    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;
    private MaterialButton btnSave, btnDelete;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri pickedImageUri = null;

    // Launcher to pick an image from gallery
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pickedImageUri = uri;
                    // Preview chosen image
                    Glide.with(this).load(uri).into(imageAvatar);
                }
            });

    // Launcher to request image-read permission (Android 12 and below)
    private final ActivityResultLauncher<String> requestStoragePermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    openImagePicker();
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // EdgeToEdge.enable(this);

        setContentView(R.layout.activity_profile);

        // Views
        toolbar = findViewById(R.id.toolbar);
        imageAvatar = findViewById(R.id.image_avatar);
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);

        // Toolbar back
        toolbar.setNavigationOnClickListener(v -> finish());

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // If guest, block this page and send to login
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        boolean isGuest = prefs.getBoolean("isGuest", true);
        if (isGuest || auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please sign in to view your profile", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Load current profile values
        loadProfile();

        // Pick/change avatar
        imageAvatar.setOnClickListener(v -> ensureImagePermissionAndPick());

        // Save all changes (name/email/password + avatar upload if selected)
        btnSave.setOnClickListener(v -> saveChanges());

        // Delete account entirely
        btnDelete.setOnClickListener(v -> deleteAccount());
    }

    /** Load name/email/avatar from Firestore/Auth */
    private void loadProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // Email from Auth
        etEmail.setText(user.getEmail());

        // Try name from Firestore first (where you store firstName/lastName)
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(this::applyProfileDoc)
                .addOnFailureListener(e -> {
                    // Fallback to Firebase Auth displayName
                    String dn = user.getDisplayName();
                    if (dn != null && !dn.trim().isEmpty()) etName.setText(dn);
                    // Load avatar from Auth (photoUrl) if present
                    if (user.getPhotoUrl() != null) {
                        Glide.with(this).load(user.getPhotoUrl()).into(imageAvatar);
                    }
                });
    }

    /** Map Firestore document to UI fields */
    private void applyProfileDoc(DocumentSnapshot doc) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String fullName = null;
        if (doc != null && doc.exists()) {
            String first = doc.getString("firstName");
            String last = doc.getString("lastName");
            if (first != null && last != null) fullName = first + " " + last;
            else if (first != null) fullName = first;

            // If you store avatarUrl in Firestore
            String avatarUrl = doc.getString("avatarUrl");
            if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
                Glide.with(this).load(avatarUrl).into(imageAvatar);
            }
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            String dn = user.getDisplayName();
            if (dn != null && !dn.trim().isEmpty()) fullName = dn;
        }
        if (fullName != null) etName.setText(fullName);

        // If Auth has photoUrl and we didn’t set from Firestore
        if (user.getPhotoUrl() != null && imageAvatar.getDrawable() == null) {
            Glide.with(this).load(user.getPhotoUrl()).into(imageAvatar);
        }
    }

    /** Ask for permission if needed, then launch the picker */
    private void ensureImagePermissionAndPick() {
        if (Build.VERSION.SDK_INT >= 33) {
            // Android 13+ uses READ_MEDIA_IMAGES (no prompt needed to pick with SAF, but safe to check)
            openImagePicker();
        } else {
            // Android 12 and below may need READ_EXTERNAL_STORAGE to preview picked file in some flows
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                requestStoragePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    /** Open the system picker (images only) */
    private void openImagePicker() {
        pickImageLauncher.launch("image/*");
    }

    /** Save name/email/password and upload avatar if chosen */
    private void saveChanges() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String name = getTrim(etName);
        String email = getTrim(etEmail);
        String password = getTrim(etPassword);

        // Basic UI validation
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            return;
        }

        // 1) Upload avatar first (if any), so we can include photoUrl in profile update
        if (pickedImageUri != null) {
            uploadAvatarThenContinue(user, name, email, password);
        } else {
            continueSavingProfile(user, name, email, password, null);
        }
    }

    /** Upload avatar to Firebase Storage: users/{uid}/avatar.<ext> */
    private void uploadAvatarThenContinue(FirebaseUser user, String name, String email, String password) {
        StorageReference ref = storage.getReference()
                .child("users")
                .child(user.getUid())
                .child("avatar." + getExt(pickedImageUri));

        ref.putFile(pickedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    // Save avatar URL in Firestore for your app
                    db.collection("users").document(user.getUid())
                            .update("avatarUrl", uri.toString())
                            .addOnCompleteListener(t -> {
                                continueSavingProfile(user, name, email, password, uri);
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Avatar upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Continue with name/email/password updates (Auth + Firestore) */
    private void continueSavingProfile(FirebaseUser user, String name, String email, String password, Uri avatarUrl) {
        // Update displayName/photoUrl in Firebase Auth profile (optional but nice)
        UserProfileChangeRequest.Builder up = new UserProfileChangeRequest.Builder();
        if (!name.isEmpty()) up.setDisplayName(name);
        if (avatarUrl != null) up.setPhotoUri(avatarUrl);

        user.updateProfile(up.build())
                .addOnFailureListener(e -> {
                    // non-fatal
                });

        // Update email (can require recent login)
        user.updateEmail(email)
                .addOnFailureListener(e -> {
                    // Common: FirebaseAuthRecentLoginRequiredException
                    Toast.makeText(this, "Email update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        // Update password if provided (optional)
        if (!password.isEmpty()) {
            user.updatePassword(password)
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Password update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }

        // Update Firestore fields you use in app (firstName/lastName split if needed)
        // Here we store a single "fullName"
        db.collection("users").document(user.getUid())
                .update("fullName", name, "email", email)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /** Delete Firestore doc + avatar + Auth user */
    private void deleteAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // 1) delete Firestore doc (ignore failure—continue attempts)
        db.collection("users").document(uid).delete()
                .addOnCompleteListener(t -> {
                    // 2) delete avatar in storage (if exists)
                    StorageReference avatarRef = storage.getReference().child("users").child(uid).child("avatar.jpg");
                    avatarRef.delete().addOnCompleteListener(t2 -> {
                        // 3) delete firebase auth user (may require recent login)
                        user.delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                    // Back to login
                                    startActivity(new Intent(this, LoginActivity.class)
                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                                    finish();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Delete failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    });
                });
    }

    /** Helpers */
    private String getTrim(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String getExt(Uri uri) {
        if (uri == null) return "jpg";
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        String type = cr.getType(uri);
        String ext = mime.getExtensionFromMimeType(type);
        return ext == null ? "jpg" : ext;
    }
}