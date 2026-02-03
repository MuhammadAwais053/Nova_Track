package com.example.novatrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.novatrack.models.Project;
import com.example.novatrack.utils.StatusBarHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddProjectActivity extends AppCompatActivity {

    private EditText projectTitleInput, projectDescriptionInput, projectSubjectInput, dueDateInput;
    private Button submitButton;
    private ImageView backButton, calendarIcon;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar selectedDate;
    private boolean isEditMode = false;
    private String projectId;
    private int currentProgress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);

        StatusBarHelper.setTransparentStatusBar(this, true);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        selectedDate = Calendar.getInstance();

        projectTitleInput = findViewById(R.id.projectTitleInput);
        projectDescriptionInput = findViewById(R.id.projectDescriptionInput);
        projectSubjectInput = findViewById(R.id.projectSubjectInput);
        dueDateInput = findViewById(R.id.dueDateInput);
        submitButton = findViewById(R.id.submitButton);
        backButton = findViewById(R.id.backButton);
        calendarIcon = findViewById(R.id.calendarIcon);

        checkEditMode();

        backButton.setOnClickListener(v -> finish());

        dueDateInput.setOnClickListener(v -> showDatePicker());
        calendarIcon.setOnClickListener(v -> showDatePicker());

        submitButton.setOnClickListener(v -> saveProject());
    }

    private void checkEditMode() {
        if (getIntent().hasExtra("PROJECT_ID")) {
            isEditMode = true;
            projectId = getIntent().getStringExtra("PROJECT_ID");
            projectTitleInput.setText(getIntent().getStringExtra("PROJECT_TITLE"));
            projectDescriptionInput.setText(getIntent().getStringExtra("PROJECT_DESCRIPTION"));
            projectSubjectInput.setText(getIntent().getStringExtra("PROJECT_SUBJECT"));
            dueDateInput.setText(getIntent().getStringExtra("PROJECT_DUE_DATE"));
            currentProgress = getIntent().getIntExtra("PROJECT_PROGRESS", 0);
            submitButton.setText("Update Project");
        }
    }

    private void showDatePicker() {
        int year = selectedDate.get(Calendar.YEAR);
        int month = selectedDate.get(Calendar.MONTH);
        int day = selectedDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    updateDateDisplay();
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    private void updateDateDisplay() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        dueDateInput.setText(sdf.format(selectedDate.getTime()));
    }
    private void setProjectAlarms(String title) {

            int code1 = title.hashCode();
            int code2 = title.hashCode() + 1;
            int code3 = title.hashCode() + 2;

            // Tomorrow reminder
            long tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
            Alarm.setAlarm(
                    this,
                    tomorrow,
                    "NovaTrack Reminder",
                    "You have pending projects to work on!",
                    code1
            );

            // 7 PM reminder
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 19);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            Alarm.setAlarm(
                    this,
                    calendar.getTimeInMillis(),
                    "7 PM Study Reminder",
                    "Time to work on your projects!",
                    code2
            );

            // 2 hours before deadline
            long dueMillis = selectedDate.getTimeInMillis();
            long reminderTime = dueMillis - (2 * 60 * 60 * 1000);

            if (reminderTime > System.currentTimeMillis()) {
                Alarm.setAlarm(
                        this,
                        reminderTime,
                        "Project Due Soon",
                        "Your project \"" + title + "\" is due in 2 hours!",
                        code3
                );
            }
        }



        private void saveProject() {
        String title = projectTitleInput.getText().toString().trim();
        String description = projectDescriptionInput.getText().toString().trim();
        String subject = projectSubjectInput.getText().toString().trim();
        String dueDate = dueDateInput.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter project title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Please enter project description", Toast.LENGTH_SHORT).show();
            return;
        }

        if (subject.isEmpty()) {
            Toast.makeText(this, "Please enter subject", Toast.LENGTH_SHORT).show();
            return;
        }

        if (dueDate.isEmpty() || dueDate.equals("No date selected yet")) {
            Toast.makeText(this, "Please select due date", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        long timestamp = System.currentTimeMillis();

        int progress = isEditMode ? currentProgress : 0;
        String status = progress == 100 ? "Completed" : "In Progress";

        Map<String, Object> projectData = new HashMap<>();
        projectData.put("userId", userId);
        projectData.put("title", title);
        projectData.put("description", description);
        projectData.put("subject", subject);
        projectData.put("dueDate", dueDate);
        projectData.put("progress", progress);
        projectData.put("status", status);
        projectData.put("updatedAt", timestamp);

        if (isEditMode) {
            db.collection("projects")
                    .document(projectId)
                    .update(projectData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();

                        setProjectAlarms(title);

                        finish();
                    })

                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update project", Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("projects")
                    .add(projectData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Project added successfully", Toast.LENGTH_SHORT).show();

                        setProjectAlarms(title);

                        finish();
                    })

                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add project", Toast.LENGTH_SHORT).show();
                    });
        }

    }

}