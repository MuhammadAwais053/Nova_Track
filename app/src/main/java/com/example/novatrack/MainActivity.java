package com.example.novatrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.novatrack.utils.StatusBarHelper;

public class MainActivity extends AppCompatActivity {

    private TextView welcomeText;
    private Button logoutButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StatusBarHelper.setTransparentStatusBar(this, true);

        mAuth = FirebaseAuth.getInstance();

        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            welcomeText.setText("Welcome, " + user.getEmail());
        }

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, SignInActivity.class));
            finish();
        });
    }
}