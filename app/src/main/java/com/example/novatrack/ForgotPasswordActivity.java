package com.example.novatrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.example.novatrack.utils.StatusBarHelper;
import com.example.novatrack.utils.ValidationHelper;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button sendResetLinkButton;
    private ImageView backButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        StatusBarHelper.setTransparentStatusBar(this, true);

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        sendResetLinkButton = findViewById(R.id.sendResetLinkButton);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        sendResetLinkButton.setOnClickListener(v -> sendPasswordResetEmail());
    }

    private void sendPasswordResetEmail() {
        String email = emailInput.getText().toString().trim();

        if (!ValidationHelper.isValidEmail(email)) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPasswordActivity.this, "Reset link sent to your email", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}