package com.example.novatrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.novatrack.utils.StatusBarHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditTaskActivity extends AppCompatActivity {

    private EditText taskNameInput, taskDescriptionInput, taskDeadlineInput;
    private RadioGroup priorityRadioGroup;
    private RadioButton radioHigh, radioMedium, radioLow;
    private Button updateButton;
    private ImageView backButton, calendarIcon;
    private FirebaseFirestore db;
    private Calendar selectedDate;
    private String taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_task);

        StatusBarHelper.setTransparentStatusBar(this, true);

        db = FirebaseFirestore.getInstance();
        selectedDate = Calendar.getInstance();

        taskNameInput = findViewById(R.id.taskNameInput);
        taskDescriptionInput = findViewById(R.id.taskDescriptionInput);
        taskDeadlineInput = findViewById(R.id.taskDeadlineInput);
        priorityRadioGroup = findViewById(R.id.priorityRadioGroup);
        radioHigh = findViewById(R.id.radioHigh);
        radioMedium = findViewById(R.id.radioMedium);
        radioLow = findViewById(R.id.radioLow);
        updateButton = findViewById(R.id.updateButton);
        backButton = findViewById(R.id.backButton);
        calendarIcon = findViewById(R.id.calendarIcon);

        loadTaskData();

        backButton.setOnClickListener(v -> finish());

        taskDeadlineInput.setOnClickListener(v -> showDatePicker());
        calendarIcon.setOnClickListener(v -> showDatePicker());

        updateButton.setOnClickListener(v -> updateTask());
    }

    private void loadTaskData() {
        taskId = getIntent().getStringExtra("TASK_ID");
        String taskName = getIntent().getStringExtra("TASK_NAME");
        String taskDescription = getIntent().getStringExtra("TASK_DESCRIPTION");
        String taskDeadline = getIntent().getStringExtra("TASK_DEADLINE");
        String priority = getIntent().getStringExtra("TASK_PRIORITY");

        taskNameInput.setText(taskName);
        taskDescriptionInput.setText(taskDescription);
        taskDeadlineInput.setText(taskDeadline);

        // Set priority radio button
        if (priority != null) {
            switch (priority.toLowerCase()) {
                case "high":
                    radioHigh.setChecked(true);
                    break;
                case "medium":
                    radioMedium.setChecked(true);
                    break;
                case "low":
                    radioLow.setChecked(true);
                    break;
            }
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
        taskDeadlineInput.setText(sdf.format(selectedDate.getTime()));
    }

    private void updateTask() {
        String taskName = taskNameInput.getText().toString().trim();
        String taskDescription = taskDescriptionInput.getText().toString().trim();
        String deadline = taskDeadlineInput.getText().toString().trim();

        if (taskName.isEmpty()) {
            Toast.makeText(this, "Please enter task name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (deadline.isEmpty()) {
            Toast.makeText(this, "Please select deadline", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected priority
        String priority = "medium"; // default
        int selectedId = priorityRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.radioHigh) {
            priority = "high";
        } else if (selectedId == R.id.radioMedium) {
            priority = "medium";
        } else if (selectedId == R.id.radioLow) {
            priority = "low";
        }

        // Update task in Firestore
        db.collection("tasks")
                .document(taskId)
                .update(
                        "taskName", taskName,
                        "taskDescription", taskDescription,
                        "deadline", deadline,
                        "priority", priority
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();

                    // Reschedule task reminders with new deadline
                    rescheduleTaskReminders(taskName, deadline);

                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show();
                });
    }

    private void rescheduleTaskReminders(String taskName, String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Calendar deadlineCal = Calendar.getInstance();
            deadlineCal.setTime(sdf.parse(deadline));
            long deadlineMillis = deadlineCal.getTimeInMillis();

            // Cancel old alarms and set new ones
            int code1 = taskId.hashCode();
            int code2 = taskId.hashCode() + 1;

            // 1 day before reminder
            long oneDayBefore = deadlineMillis - (24 * 60 * 60 * 1000);
            if (oneDayBefore > System.currentTimeMillis()) {
                Alarm.setAlarm(
                        this,
                        oneDayBefore,
                        "Task Reminder",
                        "Task \"" + taskName + "\" is due tomorrow!",
                        code1
                );
            }

            // 1 hour before reminder
            long oneHourBefore = deadlineMillis - (60 * 60 * 1000);
            if (oneHourBefore > System.currentTimeMillis()) {
                Alarm.setAlarm(
                        this,
                        oneHourBefore,
                        "Task Due Soon",
                        "Task \"" + taskName + "\" is due in 1 hour!",
                        code2
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}