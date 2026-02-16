package com.example.novatrack;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.novatrack.utils.StatusBarHelper;

public class TaskDetailsActivity extends AppCompatActivity {

    private TextView taskNameText, projectNameText, descriptionText, deadlineText, priorityText, statusText;
    private ImageView backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);

        StatusBarHelper.setTransparentStatusBar(this, true);

        taskNameText = findViewById(R.id.taskNameText);
        projectNameText = findViewById(R.id.projectNameText);
        descriptionText = findViewById(R.id.descriptionText);
        deadlineText = findViewById(R.id.deadlineText);
        priorityText = findViewById(R.id.priorityText);
        statusText = findViewById(R.id.statusText);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        loadTaskDetails();
    }

    private void loadTaskDetails() {
        String taskName = getIntent().getStringExtra("TASK_NAME");
        String projectName = getIntent().getStringExtra("PROJECT_NAME");
        String description = getIntent().getStringExtra("DESCRIPTION");
        String deadline = getIntent().getStringExtra("DEADLINE");
        String priority = getIntent().getStringExtra("PRIORITY");
        String status = getIntent().getStringExtra("STATUS");

        taskNameText.setText(taskName);
        projectNameText.setText("Project: " + projectName);
        descriptionText.setText(description);
        deadlineText.setText("Due: " + deadline);
        priorityText.setText("Priority: " + priority.toUpperCase());
        statusText.setText("Status: " + status.toUpperCase());

        // Set priority color
        int priorityColor;
        if (priority.equals("high")) {
            priorityColor = getResources().getColor(android.R.color.holo_red_dark);
        } else if (priority.equals("medium")) {
            priorityColor = getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            priorityColor = getResources().getColor(android.R.color.holo_green_dark);
        }
        priorityText.setTextColor(priorityColor);
    }
}