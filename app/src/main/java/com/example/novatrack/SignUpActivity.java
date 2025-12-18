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

import com.example.novatrack.utils.StatusBarHelper;
import com.example.novatrack.utils.ValidationHelper;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity {

    private EditText fullNameInput, emailInput, passwordInput, confirmPasswordInput;
    private TextView signInText;
    private ImageView passwordToggle, confirmPasswordToggle;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        StatusBarHelper.setTransparentStatusBar(this, true);

        mAuth = FirebaseAuth.getInstance();

        fullNameInput = findViewById(R.id.fullNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        Button signUpButton = findViewById(R.id.signUpButton);
        signInText = findViewById(R.id.signInText);
        passwordToggle = findViewById(R.id.passwordToggle);
        confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle);

        setupPasswordToggles();
        setupSignInText();

        signUpButton.setOnClickListener(v -> signUpUser());
    }

    private void setupPasswordToggles() {
        passwordToggle.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_visibility_off);
            } else {
                passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                passwordToggle.setImageResource(R.drawable.ic_visibility);
            }
            isPasswordVisible = !isPasswordVisible;
            passwordInput.setSelection(passwordInput.getText().length());
        });

        confirmPasswordToggle.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                confirmPasswordToggle.setImageResource(R.drawable.ic_visibility_off);
            } else {
                confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                confirmPasswordToggle.setImageResource(R.drawable.ic_visibility);
            }
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            confirmPasswordInput.setSelection(confirmPasswordInput.getText().length());
        });
    }

    private void setupSignInText() {
        String fullText = getString(R.string.already_have_account);
        SpannableString spannableString = new SpannableString(fullText);

        String clickableText = "Sign In";
        int start = fullText.indexOf(clickableText);
        int end = start + clickableText.length();

        if (start != -1) {
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    finish();
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(getResources().getColor(R.color.primary_blue));
                    ds.setUnderlineText(false);
                }
            };

            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        signInText.setText(spannableString);
        signInText.setMovementMethod(LinkMovementMethod.getInstance());
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
                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
