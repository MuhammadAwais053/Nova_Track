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
    private final String API_KEY = "ADD YOUR GEMINI API KEY";

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
        if (title.isEmpty() || description.isEmpty() || subject.isEmpty() || dueDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();
        int progress = isEditMode ? currentProgress : 0;
        Map<String, Object> projectData = new HashMap<>();
        projectData.put("userId", userId);
        projectData.put("title", title);
        projectData.put("description", description);
        projectData.put("subject", subject);
        projectData.put("dueDate", dueDate);
        projectData.put("progress", progress);
        projectData.put("status", progress == 100 ? "Completed" : "In Progress");
        projectData.put("updatedAt", System.currentTimeMillis());
        if (isEditMode) {
            db.collection("projects").document(projectId).update(projectData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Project updated", Toast.LENGTH_SHORT).show();
                        setProjectAlarms(title);
                        finish();
                    }).addOnFailureListener(e -> Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show());
        } else {
            db.collection("projects").add(projectData)
                    .addOnSuccessListener(doc -> {
                        projectId = doc.getId();
                        Toast.makeText(this, "Project added successfully", Toast.LENGTH_SHORT).show();
                        setProjectAlarms(title);
                        generateAITasks(projectId, title, description, subject, dueDate);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }
    }

    private void generateAITasks(String projectId, String title, String description, String subject, String dueDate) {
        Log.d(TAG, "========================================");
        Log.d(TAG, "STARTING TASK GENERATION");
        Log.d(TAG, "Project ID: " + projectId);
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "========================================");

        progressDialog.setMessage("Generating tasks...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
        Log.d(TAG, "API URL: " + url);

        String prompt = "Create exactly 6 tasks for this project. Return ONLY a valid JSON array.\n\n" +
                "Project: " + title + "\nDescription: " + description + "\nSubject: " + subject + "\nDue Date: " + dueDate + "\n\n" +
                "Format:\n[{\"taskName\":\"Research\",\"description\":\"Details\",\"priority\":\"high\",\"deadline\":\"Feb 10, 2026\"}]\n\n" +
                "Rules: 6 tasks, deadlines BEFORE " + dueDate + ", priority: high/medium/low, pure JSON\n\nTasks:";

        Log.d(TAG, "Prompt length: " + prompt.length() + " chars");

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

            Log.d(TAG, "Request body created successfully");
            Log.d(TAG, "Body: " + body.toString());

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, body,
                    response -> {
                        Log.d(TAG, "========================================");
                        Log.d(TAG, "✓ API RESPONSE RECEIVED!");
                        Log.d(TAG, "Response: " + response.toString());
                        Log.d(TAG, "========================================");
                        handleAIResponse(response, projectId, title);
                    },
                    error -> {
                        Log.e(TAG, "========================================");
                        Log.e(TAG, "✗ API ERROR!");
                        Log.e(TAG, "Error: " + error.toString());
                        if (error.networkResponse != null) {
                            Log.e(TAG, "Status Code: " + error.networkResponse.statusCode);
                            try {
                                String errorBody = new String(error.networkResponse.data);
                                Log.e(TAG, "Error Body: " + errorBody);
                            } catch (Exception e) {
                                Log.e(TAG, "Can't read error body", e);
                            }
                        } else {
                            Log.e(TAG, "No network response - check internet connection");
                            if (error.getCause() != null) {
                                Log.e(TAG, "Cause: " + error.getCause().toString());
                            }
                        }
                        Log.e(TAG, "========================================");
                        handleAIError(error);
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("X-goog-api-key", API_KEY);
                    Log.d(TAG, "Headers set: Content-Type and X-goog-api-key");
                    return headers;
                }
            };
            request.setRetryPolicy(new com.android.volley.DefaultRetryPolicy(
                    40000, // 40 seconds timeout
                    0,
                    com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));


            request.setShouldCache(false);
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(request);
            Log.d(TAG, "Request added to queue");

        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "========================================");
            Log.e(TAG, "Exception creating request", e);
            Log.e(TAG, "Exception message: " + e.getMessage());
            Log.e(TAG, "========================================");
            Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleAIResponse(JSONObject response, String projectId, String projectName) {
        progressDialog.dismiss();
        try {
            Log.d(TAG, "Parsing response...");

            JSONArray candidates = response.optJSONArray("candidates");
            Log.d(TAG, "Candidates: " + (candidates != null ? candidates.length() : "null"));

            if (candidates == null || candidates.length() == 0) {
                Log.e(TAG, "No candidates in response");
                Toast.makeText(this, "No AI response received", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject contentObj = candidate.optJSONObject("content");
            Log.d(TAG, "Content object: " + (contentObj != null ? "exists" : "null"));

            if (contentObj == null) {
                Log.e(TAG, "No content object");
                Toast.makeText(this, "Invalid response format", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            JSONArray partsArray = contentObj.optJSONArray("parts");
            Log.d(TAG, "Parts array: " + (partsArray != null ? partsArray.length() : "null"));

            if (partsArray == null || partsArray.length() == 0) {
                Log.e(TAG, "No parts in content");
                Toast.makeText(this, "Empty response content", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            String result = partsArray.getJSONObject(0).optString("text", "");
            Log.d(TAG, "Raw text length: " + result.length());
            Log.d(TAG, "Raw text: " + result);

            if (result.isEmpty()) {
                Log.e(TAG, "Empty text in response");
                Toast.makeText(this, "AI returned empty text", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            result = result.replaceAll("```json", "").replaceAll("```", "").trim();
            int start = result.indexOf('[');
            int end = result.lastIndexOf(']');
            if (start != -1 && end != -1) {
                result = result.substring(start, end + 1);
            }

            Log.d(TAG, "Cleaned JSON: " + result);

            JSONArray tasks = new JSONArray(result);
            Log.d(TAG, "✓ Parsed " + tasks.length() + " tasks successfully!");

            saveTasksToFirestore(projectId, projectName, tasks);

        } catch (Exception e) {
            Log.e(TAG, "Parse exception", e);
            Log.e(TAG, "Exception message: " + e.getMessage());
            Toast.makeText(this, "Parse error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void handleAIError(com.android.volley.VolleyError error) {
        progressDialog.dismiss();
        String msg = "Unknown error";

        if (error.networkResponse != null) {
            int code = error.networkResponse.statusCode;
            Log.e(TAG, "HTTP Status Code: " + code);

            try {
                String errorBody = new String(error.networkResponse.data);
                Log.e(TAG, "Error response: " + errorBody);
            } catch (Exception e) {
                Log.e(TAG, "Can't read error response", e);
            }

            switch (code) {
                case 400:
                    msg = "Bad request - check API format";
                    break;
                case 403:
                    msg = "API key denied - check permissions";
                    break;
                case 404:
                    msg = "Model not found - gemini-2.5-flash unavailable";
                    break;
                case 429:
                    msg = "Rate limit exceeded. Create new API key at aistudio.google.com";
                    break;
                case 500:
                    msg = "Server error - try again";
                    break;
                default:
                    msg = "HTTP Error " + code;
            }
        } else if (error.getMessage() != null) {
            msg = "Network error: " + error.getMessage();
            Log.e(TAG, "Network error message: " + error.getMessage());
        } else {
            msg = "No network response - check internet";
            Log.e(TAG, "No network response at all");
        }

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        finish();
    }

    private void saveTasksToFirestore(String projectId, String projectName, JSONArray tasksArray) {
        String userId = mAuth.getCurrentUser().getUid();
        int count = Math.min(tasksArray.length(), 6);

        Log.d(TAG, "========================================");
        Log.d(TAG, "SAVING TASKS TO FIRESTORE");
        Log.d(TAG, "Project ID: " + projectId);
        Log.d(TAG, "Tasks count: " + count);
        Log.d(TAG, "========================================");

        for (int i = 0; i < count; i++) {
            try {
                JSONObject taskJson = tasksArray.getJSONObject(i);
                String taskName = taskJson.optString("taskName", "Task " + (i + 1));
                String taskDesc = taskJson.optString("description", "");
                String priority = taskJson.optString("priority", "medium").toLowerCase();
                String deadline = taskJson.optString("deadline", dueDateInput.getText().toString());

                if (!priority.matches("high|medium|low")) priority = "medium";

                Log.d(TAG, "Creating task " + (i+1) + ": " + taskName);

                Task task = new Task(projectId, projectName, taskName, taskDesc,
                        deadline, "pending", priority, System.currentTimeMillis(), userId);

                final int taskNum = i + 1;
                db.collection("tasks").add(task)
                        .addOnSuccessListener(doc -> {
                            Log.d(TAG, "✓ Task " + taskNum + " saved: " + doc.getId());
                            scheduleTaskReminders(doc.getId(), taskName, deadline);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "✗ Task " + taskNum + " failed", e);
                        });

            } catch (Exception e) {
                Log.e(TAG, "Error creating task " + (i+1), e);
            }
        }

        Toast.makeText(this, "✓ Tasks generated successfully!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "========================================");
        Log.d(TAG, "TASK GENERATION COMPLETE!");
        Log.d(TAG, "========================================");
        finish();
    }

    private void scheduleTaskReminders(String taskId, String taskName, String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(deadline));
            long deadlineMillis = cal.getTimeInMillis();
            long oneDayBefore = deadlineMillis - 86400000;
            long oneHourBefore = deadlineMillis - 3600000;
            if (oneDayBefore > System.currentTimeMillis()) {
                Alarm.setAlarm(this, oneDayBefore, "Task Reminder",
                        "Task \"" + taskName + "\" is due tomorrow!", taskId.hashCode());
            }
            if (oneHourBefore > System.currentTimeMillis()) {
                Alarm.setAlarm(this, oneHourBefore, "Task Due Soon",
                        "Task \"" + taskName + "\" is due in 1 hour!", taskId.hashCode() + 1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Reminder scheduling error", e);
        }
    }
}