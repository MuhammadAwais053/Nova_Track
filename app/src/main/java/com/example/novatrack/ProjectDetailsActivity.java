package com.example.novatrack;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.novatrack.adapters.TaskAdapter;
import com.example.novatrack.models.Task;
import com.example.novatrack.utils.StatusBarHelper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ProjectDetailsActivity extends AppCompatActivity {

    private TextView projectTitleText, descriptionText, subjectText, dueDateText, progressText;
    private ProgressBar progressBar;
    private RecyclerView tasksRecyclerView;
    private TextView emptyTasksView;
    private ImageView backButton;
    private TaskAdapter taskAdapter;
    private List<Task> tasksList;
    private FirebaseFirestore db;
    private String projectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_details);

        StatusBarHelper.setTransparentStatusBar(this, true);

        db = FirebaseFirestore.getInstance();

        projectTitleText = findViewById(R.id.projectTitleText);
        descriptionText = findViewById(R.id.descriptionText);
        subjectText = findViewById(R.id.subjectText);
        dueDateText = findViewById(R.id.dueDateText);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
        tasksRecyclerView = findViewById(R.id.tasksRecyclerView);
        emptyTasksView = findViewById(R.id.emptyTasksView);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        tasksList = new ArrayList<>();
        taskAdapter = new TaskAdapter(tasksList, new TaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskChecked(Task task, boolean isChecked) {
                // Handle task completion
            }

            @Override
            public void onTaskLongClick(Task task) {
                // Handle long click
            }
        });

        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);

        loadProjectDetails();
        loadProjectTasks();
    }

    private void loadProjectDetails() {
        projectId = getIntent().getStringExtra("PROJECT_ID");
        String title = getIntent().getStringExtra("PROJECT_TITLE");
        String description = getIntent().getStringExtra("PROJECT_DESCRIPTION");
        String subject = getIntent().getStringExtra("PROJECT_SUBJECT");
        String dueDate = getIntent().getStringExtra("PROJECT_DUE_DATE");
        int progress = getIntent().getIntExtra("PROJECT_PROGRESS", 0);

        projectTitleText.setText(title);
        descriptionText.setText(description);
        subjectText.setText("Subject: " + subject);
        dueDateText.setText("Due: " + dueDate);
        progressText.setText(progress + "%");
        progressBar.setProgress(progress);
    }

    private void loadProjectTasks() {
        db.collection("tasks")
                .whereEqualTo("projectId", projectId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    tasksList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Task task = doc.toObject(Task.class);
                            task.setTaskId(doc.getId());
                            tasksList.add(task);
                        }
                    }

                    taskAdapter.notifyDataSetChanged();
                    updateEmptyView();
                });
    }

    private void updateEmptyView() {
        if (tasksList.isEmpty()) {
            emptyTasksView.setVisibility(View.VISIBLE);
            tasksRecyclerView.setVisibility(View.GONE);
        } else {
            emptyTasksView.setVisibility(View.GONE);
            tasksRecyclerView.setVisibility(View.VISIBLE);
        }
    }
}