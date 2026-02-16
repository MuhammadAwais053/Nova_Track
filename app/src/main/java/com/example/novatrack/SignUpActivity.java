package com.example.novatrack;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.novatrack.utils.StatusBarHelper;
import com.example.novatrack.utils.ValidationHelper;
import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, passwordInput, confirmPasswordInput;
    private Button signUpButton;
    private TextView signInText;
    private ImageView passwordToggle, confirmPasswordToggle;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_signup);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading layout: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        StatusBarHelper.setTransparentStatusBar(this, true);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        try {
            fullNameInput = findViewById(R.id.fullNameInput);
            emailInput = findViewById(R.id.emailInput);
            passwordInput = findViewById(R.id.passwordInput);
            confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
            signUpButton = findViewById(R.id.signUpButton);
            signInText = findViewById(R.id.signInText);
            passwordToggle = findViewById(R.id.passwordToggle);
            confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle);

            setupPasswordToggles();
            setupSignInText();

            signUpButton.setOnClickListener(v -> signUpUser());
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupPasswordToggles() {
        if (passwordToggle != null) {
            passwordToggle.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    try {
                        passwordToggle.setImageResource(R.drawable.ic_visibility_off);
                    } catch (Exception e) {
                        // Ignore if drawable not found
                    }
                } else {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    try {
                        passwordToggle.setImageResource(R.drawable.ic_visibility);
                    } catch (Exception e) {
                        // Ignore if drawable not found
                    }
                }
                isPasswordVisible = !isPasswordVisible;
                passwordInput.setSelection(passwordInput.getText().length());
            });
        }

        if (confirmPasswordToggle != null) {
            confirmPasswordToggle.setOnClickListener(v -> {
                if (isConfirmPasswordVisible) {
                    confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    try {
                        confirmPasswordToggle.setImageResource(R.drawable.ic_visibility_off);
                    } catch (Exception e) {
                        // Ignore if drawable not found
                    }
                } else {
                    confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    try {
                        confirmPasswordToggle.setImageResource(R.drawable.ic_visibility);
                    } catch (Exception e) {
                        // Ignore if drawable not found
                    }
                }
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
                confirmPasswordInput.setSelection(confirmPasswordInput.getText().length());
            });
        }
    }

    private void setupSignInText() {
        try {
            String text = "Already have account? Sign In";
            SpannableString spannableString = new SpannableString(text);

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    finish();
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    try {
                        ds.setColor(getResources().getColor(R.color.primary_blue));
                    } catch (Exception e) {
                        ds.setColor(0xFF4285F4);
                    }
                    ds.setUnderlineText(false);
                }
            };

            spannableString.setSpan(clickableSpan, 23, 30, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            signInText.setText(spannableString);
            signInText.setMovementMethod(LinkMovementMethod.getInstance());
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback: simple click listener
            signInText.setOnClickListener(v -> finish());
        }
    }

    private void signUpUser() {
        String fullName = fullNameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationHelper.isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationHelper.isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Save user info to Firestore
                        String userId = mAuth.getCurrentUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", fullName);
                        user.put("email", email);
                        user.put("createdAt", System.currentTimeMillis());

                        db.collection("users").document(userId).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(SignUpActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();

                                    Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(SignUpActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                    // Still navigate to home even if Firestore save fails
                                    Intent intent = new Intent(SignUpActivity.this, HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                    } else {
                        Toast.makeText(SignUpActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}