package com.example.novatrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.novatrack.adapters.ProjectAdapter;
import com.example.novatrack.models.Project;
import com.example.novatrack.utils.StatusBarHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProjectDashboardActivity extends AppCompatActivity {

    private RecyclerView projectsRecyclerView;
    private ProjectAdapter projectAdapter;
    private List<Project> projectList;
    private List<Project> filteredProjectList;
    private FloatingActionButton fabAddProject;
    private EditText searchInput;
    private TextView emptyView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_dashboard);

        StatusBarHelper.setTransparentStatusBar(this, true);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        projectsRecyclerView = findViewById(R.id.projectsRecyclerView);
        fabAddProject = findViewById(R.id.fabAddProject);
        searchInput = findViewById(R.id.searchInput);
        emptyView = findViewById(R.id.emptyView);

        projectList = new ArrayList<>();
        filteredProjectList = new ArrayList<>();

        projectAdapter = new ProjectAdapter(filteredProjectList, new ProjectAdapter.OnProjectLongClickListener() {
            @Override
            public void onProjectLongClick(Project project, View view) {
                showProjectOptionsDialog(project);
            }
        });

        projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        projectsRecyclerView.setAdapter(projectAdapter);

        fabAddProject.setOnClickListener(v -> {
            Intent intent = new Intent(ProjectDashboardActivity.this, AddProjectActivity.class);
            startActivity(intent);
        });

        setupSearchFunctionality();
        loadProjects();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Logout", (dialog, which) -> {
                        mAuth.signOut();
                        Intent intent = new Intent(ProjectDashboardActivity.this, SignInActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupSearchFunctionality() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProjects(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void filterProjects(String query) {
        filteredProjectList.clear();
        if (query.isEmpty()) {
            filteredProjectList.addAll(projectList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Project project : projectList) {
                if (project.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        project.getSubject().toLowerCase().contains(lowerCaseQuery)) {
                    filteredProjectList.add(project);
                }
            }
        }
        projectAdapter.notifyDataSetChanged();
        updateEmptyView();
    }

    private void loadProjects() {
        db.collection("projects")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading projects", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    projectList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Project project = doc.toObject(Project.class);
                            project.setId(doc.getId());
                            projectList.add(project);

                            // Schedule notification for this project
                            scheduleProjectReminders(project);
                        }
                    }

                    filteredProjectList.clear();
                    filteredProjectList.addAll(projectList);
                    projectAdapter.notifyDataSetChanged();
                    updateEmptyView();
                });
    }

    private void updateEmptyView() {
        if (filteredProjectList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            projectsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            projectsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showProjectOptionsDialog(Project project) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(project.getTitle());

        String[] options = {"Edit", "Delete"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) editProject(project);
            else if (which == 1) confirmDeleteProject(project);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void editProject(Project project) {
        Intent intent = new Intent(ProjectDashboardActivity.this, AddProjectActivity.class);
        intent.putExtra("PROJECT_ID", project.getId());
        intent.putExtra("PROJECT_TITLE", project.getTitle());
        intent.putExtra("PROJECT_DESCRIPTION", project.getDescription());
        intent.putExtra("PROJECT_SUBJECT", project.getSubject());
        intent.putExtra("PROJECT_DUE_DATE", project.getDueDate());
        intent.putExtra("PROJECT_PROGRESS", project.getProgress());
        startActivity(intent);
    }

    private void confirmDeleteProject(Project project) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete \"" + project.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> deleteProject(project))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProject(Project project) {
        db.collection("projects")
                .document(project.getId())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Project deleted", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete project", Toast.LENGTH_SHORT).show());
    }

    // ======== NOTIFICATIONS ========

    private void scheduleProjectNotification(String title, String message, long triggerAtMillis) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("notification_title", title);
        intent.putExtra("notification_text", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(), // unique request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private void scheduleProjectReminders(Project project) {
        long dueMillis = getMillisFromDueDate(project.getDueDate());
        if (dueMillis > System.currentTimeMillis()) { // only schedule future notifications
            String title = "Project Reminder";
            String message = "Your project \"" + project.getTitle() + "\" is due soon!";
            scheduleProjectNotification(title, message, dueMillis);
        }
    }

    // Convert project due date String to milliseconds
    private long getMillisFromDueDate(String dueDate) {
        // Example format: "2026-02-01 15:30"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        try {
            Date date = sdf.parse(dueDate);
            if (date != null) return date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjects();
    }
}
