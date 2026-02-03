package com.example.novatrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_screen);

        // Welcome Text
        TextView tvWelcome = findViewById(R.id.Greeting);
        String username = getIntent().getStringExtra("USERNAME");

        if (username != null && !username.isEmpty()) {
            tvWelcome.setText("Hi " + username);
        } else {
            tvWelcome.setText("Hi User");
        }

        // Chat input
        EditText searchBox = findViewById(R.id.projectTitleInput);

        searchBox.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {

                String userText = searchBox.getText().toString().trim();

                if (!userText.isEmpty()) {
                    // Open ChatbotActivity and pass the message
                    Intent intent = new Intent(HomeActivity.this, ChatbotActivity.class);
                    intent.putExtra("USER_MESSAGE", userText);
                    startActivity(intent);

                    searchBox.setText(""); // clear input
                }

                return true;
            }
            return false;
        });

        Button btnProject = findViewById(R.id.btnProject);
        btnProject.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ProjectDashboardActivity.class);
            startActivity(intent);
        });
    }
}
