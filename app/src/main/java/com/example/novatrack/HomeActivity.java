package com.example.novatrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private Button btnProjects, btnTasks, btnProgress, btnCalendar;
    private EditText searchBox;
    private TextView tvWelcome;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);

        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        tvWelcome = findViewById(R.id.Greeting);
        searchBox = findViewById(R.id.projectTitleInput);
        btnProjects = findViewById(R.id.btnProject);
        btnTasks = findViewById(R.id.btnTasks);
        btnProgress = findViewById(R.id.btnProgress);
        btnCalendar = findViewById(R.id.btnCalendar);

        // Set welcome message
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email != null && email.contains("@")) {
                String username = email.substring(0, email.indexOf("@"));
                tvWelcome.setText("Hi " + username);
            } else {
                tvWelcome.setText("Hi User");
            }
        } else {
            tvWelcome.setText("Hi User");
        }

        // Chat input - opens Chatbot
        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                String userText = searchBox.getText().toString().trim();

                if (!userText.isEmpty()) {
                    Intent intent = new Intent(HomeActivity.this, ChatbotActivity.class);
                    intent.putExtra("USER_MESSAGE", userText);
                    startActivity(intent);
                    searchBox.setText("");
                }
                return true;
            }
            return false;
        });

        // PROJECTS button - opens ProjectDashboardActivity
        btnProjects.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProjectDashboardActivity.class);
            startActivity(intent);
        });

        // TASKS button - opens TaskActivity
        btnTasks.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TaskActivity.class);
            startActivity(intent);
        });

        // PROGRESS button - opens ProgressActivity
        btnProgress.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProgressActivity.class);
            startActivity(intent);
        });

        // CALENDAR button - opens CalendarActivity
        btnCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalendarActivity.class);
            startActivity(intent);
        });
    }
}