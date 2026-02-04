package com.example.novatrack;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.novatrack.adapters.TaskAdapter;
import com.example.novatrack.models.Task;
import com.example.novatrack.utils.StatusBarHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskActivity extends AppCompatActivity {

    private static final String TAG = "TaskActivity";
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> allTasksList;
    private List<Task> filteredTasksList;
    private ChipGroup filterChipGroup;
    private TextView emptyView;
    private ImageView backButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        Log.d(TAG, "=== TaskActivity onCreate ===");

        StatusBarHelper.setTransparentStatusBar(this, true);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Log.e(TAG, "User not logged in!");
            Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        emptyView = findViewById(R.id.emptyView);
        backButton = findViewById(R.id.backButton);

        allTasksList = new ArrayList<>();
        filteredTasksList = new ArrayList<>();

        taskAdapter = new TaskAdapter(filteredTasksList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskChecked(Task task, boolean isChecked) {
                updateTaskStatus(task, isChecked);
            }

            @Override
            public void onTaskLongClick(Task task) {
                showTaskOptionsDialog(task);
            }
        });

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        backButton.setOnClickListener(v -> finish());

        setupFilterChips();
        loadTasks();
    }

    private void setupFilterChips() {
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                Chip chip = findViewById(checkedId);
                if (chip != null) {
                    String filter = chip.getText().toString().toLowerCase();
                    currentFilter = filter;
                    Log.d(TAG, "Filter changed to: " + filter);
                    filterTasks(filter);
                }
            }
        });

        Chip allChip = findViewById(R.id.chipAll);
        if (allChip != null) {
            allChip.setChecked(true);
        }
    }

    private void loadTasks() {
        String userId = mAuth.getCurrentUser().getUid();

        Log.d(TAG, "=== LOADING TASKS ===");
        Log.d(TAG, "User ID: " + userId);

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading tasks", error);
                        Toast.makeText(this, "Error loading tasks: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    allTasksList.clear();

                    if (value != null) {
                        Log.d(TAG, "Documents received: " + value.size());

                        for (QueryDocumentSnapshot doc : value) {
                            try {
                                Task task = doc.toObject(Task.class);
                                task.setTaskId(doc.getId());
                                allTasksList.add(task);

                                Log.d(TAG, "Loaded task: " + task.getTaskName() + " (Project: " + task.getProjectName() + ")");
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing task document: " + doc.getId(), e);
                            }
                        }

                        Log.d(TAG, "Total tasks loaded: " + allTasksList.size());
                    } else {
                        Log.d(TAG, "No documents in snapshot");
                    }

                    filterTasks(currentFilter);
                });
    }

    private void filterTasks(String filter) {
        Log.d(TAG, "Filtering tasks with: " + filter);

        filteredTasksList.clear();

        switch (filter.toLowerCase()) {
            case "all":
                filteredTasksList.addAll(allTasksList);
                break;

            case "today":
                for (Task task : allTasksList) {
                    if (isToday(task.getDeadline())) {
                        filteredTasksList.add(task);
                    }
                }
                break;

            case "this week":
                for (Task task : allTasksList) {
                    if (isThisWeek(task.getDeadline())) {
                        filteredTasksList.add(task);
                    }
                }
                break;

            case "overdue":
                for (Task task : allTasksList) {
                    if (!task.getStatus().equals("completed") && isOverdue(task.getDeadline())) {
                        filteredTasksList.add(task);
                    }
                }
                break;
        }

        Log.d(TAG, "Filtered tasks count: " + filteredTasksList.size());

        taskAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private boolean isToday(String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date taskDate = sdf.parse(deadline);
            if (taskDate == null) return false;

            Calendar taskCal = Calendar.getInstance();
            taskCal.setTime(taskDate);

            Calendar todayCal = Calendar.getInstance();

            return taskCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    taskCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + deadline, e);
            return false;
        }
    }

    private boolean isThisWeek(String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date taskDate = sdf.parse(deadline);
            if (taskDate == null) return false;

            Date today = new Date();
            Calendar weekEnd = Calendar.getInstance();
            weekEnd.setTime(today);
            weekEnd.add(Calendar.DAY_OF_YEAR, 7);

            return taskDate.after(today) && taskDate.before(weekEnd.getTime());
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + deadline, e);
            return false;
        }
    }

    private boolean isOverdue(String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date taskDate = sdf.parse(deadline);
            Date today = new Date();
            return taskDate != null && taskDate.before(today);
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing date: " + deadline, e);
            return false;
        }
    }

    private void updateTaskStatus(Task task, boolean isCompleted) {
        Log.d(TAG, "Updating task: " + task.getTaskName() + " to " + (isCompleted ? "completed" : "pending"));

        String newStatus = isCompleted ? "completed" : "pending";
        long completedAt = isCompleted ? System.currentTimeMillis() : 0;

        db.collection("tasks")
                .document(task.getTaskId())
                .update("status", newStatus, "completedAt", completedAt)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task status updated successfully");
                    Toast.makeText(this, isCompleted ? "Task completed!" : "Task marked as pending", Toast.LENGTH_SHORT).show();
                    updateProjectProgress(task.getProjectId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update task status", e);
                    Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProjectProgress(String projectId) {
        Log.d(TAG, "Updating project progress for: " + projectId);

        db.collection("tasks")
                .whereEqualTo("projectId", projectId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalTasks = 0;
                    int completedTasks = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        totalTasks++;
                        Task task = doc.toObject(Task.class);
                        if (task.getStatus().equals("completed")) {
                            completedTasks++;
                        }
                    }

                    int progress = totalTasks > 0 ? (completedTasks * 100) / totalTasks : 0;
                    String status = progress == 100 ? "Completed" : "In Progress";

                    Log.d(TAG, "Project progress: " + progress + "% (" + completedTasks + "/" + totalTasks + ")");

                    db.collection("projects")
                            .document(projectId)
                            .update("progress", progress, "status", status)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Project progress updated"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to update project progress", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to query tasks", e));
    }

    private void showTaskOptionsDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(task.getTaskName());

        String[] options = {"Edit", "Delete"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                editTask(task);
            } else if (which == 1) {
                confirmDeleteTask(task);
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void editTask(Task task) {
        Intent intent = new Intent(TaskActivity.this, EditTaskActivity.class);
        intent.putExtra("TASK_ID", task.getTaskId());
        intent.putExtra("TASK_NAME", task.getTaskName());
        intent.putExtra("TASK_DESCRIPTION", task.getTaskDescription());
        intent.putExtra("TASK_DEADLINE", task.getDeadline());
        intent.putExtra("TASK_PRIORITY", task.getPriority());
        startActivity(intent);
    }

    private void confirmDeleteTask(Task task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete \"" + task.getTaskName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask(task))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask(Task task) {
        Log.d(TAG, "Deleting task: " + task.getTaskName());

        db.collection("tasks")
                .document(task.getTaskId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted successfully");
                    Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
                    updateProjectProgress(task.getProjectId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete task", e);
                    Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyView() {
        if (filteredTasksList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);

            switch (currentFilter.toLowerCase()) {
                case "today":
                    emptyView.setText("No tasks due today");
                    break;
                case "this week":
                    emptyView.setText("No tasks due this week");
                    break;
                case "overdue":
                    emptyView.setText("No overdue tasks");
                    break;
                default:
                    emptyView.setText("No tasks yet.\nAdd a project to get started!");
                    break;
            }
        } else {
            emptyView.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - reloading tasks");
        loadTasks();
    }
}