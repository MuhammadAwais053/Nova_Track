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

public class SignInActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private TextView signUpText, forgotPasswordText;
    private ImageView passwordToggle;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        StatusBarHelper.setTransparentStatusBar(this, true);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        Button signInButton = findViewById(R.id.signInButton);
        signUpText = findViewById(R.id.signUpText);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        passwordToggle = findViewById(R.id.passwordToggle);

        setupPasswordToggle();
        setupSignUpText();
        setupForgotPasswordText();

        signInButton.setOnClickListener(v -> signInUser());
    }

    private void setupPasswordToggle() {
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
    }

    private void setupSignUpText() {
        String fullText = getString(R.string.dont_have_account);
        SpannableString spannableString = new SpannableString(fullText);

        String clickableText = "Sign Up";
        int start = fullText.indexOf(clickableText);
        int end = start + clickableText.length();

        if (start != -1) {
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    startActivity(new Intent(SignInActivity.this, SignUpActivity.class));
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setColor(getResources().getColor(R.color.primary_blue));
                    ds.setUnderlineText(false);
                }
            };

            spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        signUpText.setText(spannableString);
        signUpText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupForgotPasswordText() {
        forgotPasswordText.setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, ForgotPasswordActivity.class))
        );
    }

    private void signInUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!ValidationHelper.isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!ValidationHelper.isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this,
                                "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
