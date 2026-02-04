package com.example.novatrack;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.novatrack.models.Task;
import com.example.novatrack.utils.StatusBarHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddProjectActivity extends AppCompatActivity {

    private static final String TAG = "AddProjectActivity";
    private EditText projectTitleInput, projectDescriptionInput, projectSubjectInput, dueDateInput;
    private Button submitButton;
    private ImageView backButton, calendarIcon;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Calendar selectedDate;
    private boolean isEditMode = false;
    private String projectId;
    private int currentProgress = 0;
    private ProgressDialog progressDialog;
    private final String API_KEY = "AIzaSyCS_Qi0pdcZsdo43Wxvyrcrcuy1lu7Su-w";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_project);
        StatusBarHelper.setTransparentStatusBar(this, true);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        selectedDate = Calendar.getInstance();
        progressDialog = new ProgressDialog(this);
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
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);
                    updateDateDisplay();
                }, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
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
        long tomorrow = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
        Alarm.setAlarm(this, tomorrow, "NovaTrack Reminder", "You have pending projects to work on!", code1);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 19);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        Alarm.setAlarm(this, calendar.getTimeInMillis(), "7 PM Study Reminder", "Time to work on your projects!", code2);
        long dueMillis = selectedDate.getTimeInMillis();
        long reminderTime = dueMillis - (2 * 60 * 60 * 1000);
        if (reminderTime > System.currentTimeMillis()) {
            Alarm.setAlarm(this, reminderTime, "Project Due Soon", "Your project \"" + title + "\" is due in 2 hours!", code3);
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
        if (dueDate.isEmpty()) {
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
            db.collection("projects").document(projectId).update(projectData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Project updated successfully", Toast.LENGTH_SHORT).show();
                        setProjectAlarms(title);
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update project", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Update failed", e);
                    });
        } else {
            db.collection("projects").add(projectData)
                    .addOnSuccessListener(documentReference -> {
                        projectId = documentReference.getId();
                        Toast.makeText(this, "Project added successfully", Toast.LENGTH_SHORT).show();
                        setProjectAlarms(title);
                        generateAITasks(projectId, title, description, subject, dueDate);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to add project", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Save failed", e);
                        finish();
                    });
        }
    }

    private void generateAITasks(String projectId, String title, String description, String subject, String dueDate) {
        progressDialog.setMessage("Generating tasks...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Add small delay to avoid rate limit
        new android.os.Handler().postDelayed(() -> {
            generateAITasksNow(projectId, title, description, subject, dueDate);
        }, 1000); // 1 second delay
    }

    private void generateAITasksNow(String projectId, String title, String description, String subject, String dueDate) {
        // ✅ CORRECT: Using X-goog-api-key header (NOT in URL!)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

        Log.d(TAG, "Using gemini-2.0-flash with X-goog-api-key header");

        String prompt = "Create exactly 6 tasks for this project. Return ONLY a valid JSON array.\n\n" +
                "Project: " + title + "\nDescription: " + description + "\nSubject: " + subject + "\nDue Date: " + dueDate + "\n\n" +
                "Format (NO markdown):\n[{\"taskName\":\"Task 1\",\"description\":\"Details\",\"priority\":\"high\",\"deadline\":\"Feb 10, 2026\"}]\n\n" +
                "Rules:\n- Exactly 6 tasks\n- All deadlines BEFORE " + dueDate + "\n- Priority: high, medium, or low\n- Pure JSON only\n\nTasks:";

        try {
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            JSONArray parts = new JSONArray();
            parts.put(textPart);
            JSONObject content = new JSONObject();
            content.put("parts", parts);
            JSONArray contents = new JSONArray();
            contents.put(content);
            JSONObject body = new JSONObject();
            body.put("contents", contents);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> handleAIResponse(response, projectId, title),
                    this::handleAIError) {

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("X-goog-api-key", API_KEY); // ✅ API KEY IN HEADER!
                    return headers;
                }
            };

            request.setShouldCache(false);
            Volley.newRequestQueue(this).add(request);

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "Request error", e);
            Toast.makeText(this, "Failed to generate", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleAIResponse(JSONObject response, String projectId, String projectName) {
        progressDialog.dismiss();
        try {
            JSONArray candidates = response.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) {
                Toast.makeText(this, "No response", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject contentObj = candidate.optJSONObject("content");
            if (contentObj == null) {
                Toast.makeText(this, "Invalid format", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            JSONArray partsArray = contentObj.optJSONArray("parts");
            if (partsArray == null || partsArray.length() == 0) {
                Toast.makeText(this, "Empty content", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            String result = partsArray.getJSONObject(0).optString("text", "");
            if (result.isEmpty()) {
                Toast.makeText(this, "Empty text", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            result = result.trim().replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            int start = result.indexOf('[');
            int end = result.lastIndexOf(']');
            if (start != -1 && end != -1 && end > start) {
                result = result.substring(start, end + 1);
            }
            JSONArray tasksArray = new JSONArray(result);
            Log.d(TAG, "✓ SUCCESS: Generated " + tasksArray.length() + " tasks!");
            saveTasksToFirestore(projectId, projectName, tasksArray);
        } catch (Exception e) {
            Log.e(TAG, "Parse error", e);
            Toast.makeText(this, "Parse failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleAIError(com.android.volley.VolleyError error) {
        progressDialog.dismiss();
        String errorMsg = "Unknown error";
        if (error.networkResponse != null) {
            int code = error.networkResponse.statusCode;
            try {
                String body = new String(error.networkResponse.data);
                Log.e(TAG, "Error " + code + ": " + body);
            } catch (Exception e) {
                Log.e(TAG, "Can't read error", e);
            }
            if (code == 404) errorMsg = "Model not found";
            else if (code == 400) errorMsg = "Bad request";
            else if (code == 403) errorMsg = "API key denied";
            else if (code == 429) errorMsg = "Too many requests";
            else errorMsg = "Error " + code;
        } else if (error.getMessage() != null) {
            errorMsg = error.getMessage();
        }
        Toast.makeText(this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
        finish();
    }

    private void saveTasksToFirestore(String projectId, String projectName, JSONArray tasksArray) {
        String userId = mAuth.getCurrentUser().getUid();
        int tasksCount = Math.min(tasksArray.length(), 6);
        Log.d(TAG, "Saving " + tasksCount + " tasks");
        for (int i = 0; i < tasksCount; i++) {
            try {
                JSONObject taskJson = tasksArray.getJSONObject(i);
                String taskName = taskJson.optString("taskName", "Task " + (i + 1));
                String taskDescription = taskJson.optString("description", "");
                String priority = taskJson.optString("priority", "medium").toLowerCase();
                String deadline = taskJson.optString("deadline", dueDateInput.getText().toString());
                if (!priority.equals("high") && !priority.equals("medium") && !priority.equals("low")) {
                    priority = "medium";
                }
                Task task = new Task(projectId, projectName, taskName, taskDescription,
                        deadline, "pending", priority, System.currentTimeMillis(), userId);
                db.collection("tasks").add(task)
                        .addOnSuccessListener(doc -> {
                            Log.d(TAG, "✓ Task saved: " + taskName);
                            scheduleTaskReminders(doc.getId(), taskName, deadline);
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "✗ Failed: " + taskName, e));
            } catch (Exception e) {
                Log.e(TAG, "Task error " + i, e);
            }
        }
        Toast.makeText(this, "Tasks generated successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void scheduleTaskReminders(String taskId, String taskName, String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Calendar deadlineCal = Calendar.getInstance();
            deadlineCal.setTime(sdf.parse(deadline));
            long deadlineMillis = deadlineCal.getTimeInMillis();
            int code1 = taskId.hashCode();
            int code2 = taskId.hashCode() + 1;
            long oneDayBefore = deadlineMillis - (24 * 60 * 60 * 1000);
            if (oneDayBefore > System.currentTimeMillis()) {
                Alarm.setAlarm(this, oneDayBefore, "Task Reminder",
                        "Task \"" + taskName + "\" is due tomorrow!", code1);
            }
            long oneHourBefore = deadlineMillis - (60 * 60 * 1000);
            if (oneHourBefore > System.currentTimeMillis()) {
                Alarm.setAlarm(this, oneHourBefore, "Task Due Soon",
                        "Task \"" + taskName + "\" is due in 1 hour!", code2);
            }
        } catch (Exception e) {
            Log.e(TAG, "Reminder error", e);
        }
    }
}