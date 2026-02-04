package com.example.novatrack;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.novatrack.models.Project;
import com.example.novatrack.models.Task;
import com.example.novatrack.utils.StatusBarHelper;
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

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private RecyclerView eventsRecyclerView;
    private TextView selectedDateText, emptyView;
    private ImageView backButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Project> projectList;
    private List<Task> taskList;
    private CalendarEventsAdapter eventsAdapter;
    private List<CalendarEvent> eventsList;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        StatusBarHelper.setTransparentStatusBar(this, true);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        projectList = new ArrayList<>();
        taskList = new ArrayList<>();
        eventsList = new ArrayList<>();

        calendarView = findViewById(R.id.calendarView);
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView);
        selectedDateText = findViewById(R.id.selectedDateText);
        emptyView = findViewById(R.id.emptyView);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        // Set today's date as selected
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        selectedDate = sdf.format(new Date());
        selectedDateText.setText("Events on " + selectedDate);

        eventsAdapter = new CalendarEventsAdapter(eventsList);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsRecyclerView.setAdapter(eventsAdapter);

        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, dayOfMonth);
                selectedDate = sdf.format(calendar.getTime());
                selectedDateText.setText("Events on " + selectedDate);
                filterEventsForDate(selectedDate);
            }
        });

        loadProjectsAndTasks();
    }

    private void loadProjectsAndTasks() {
        String userId = mAuth.getCurrentUser().getUid();

        // Load projects
        db.collection("projects")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    projectList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Project project = doc.toObject(Project.class);
                        project.setId(doc.getId());
                        projectList.add(project);
                    }
                    loadTasks();
                });
    }

    private void loadTasks() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    taskList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = doc.toObject(Task.class);
                        task.setTaskId(doc.getId());
                        taskList.add(task);
                    }
                    filterEventsForDate(selectedDate);
                });
    }

    private void filterEventsForDate(String date) {
        eventsList.clear();

        // Add projects due on this date
        for (Project project : projectList) {
            if (project.getDueDate().equals(date)) {
                eventsList.add(new CalendarEvent(
                        project.getTitle(),
                        "Project Due",
                        date,
                        "project",
                        project.getStatus().equals("Completed")
                ));
            }
        }

        // Add tasks due on this date
        for (Task task : taskList) {
            if (task.getDeadline().equals(date)) {
                eventsList.add(new CalendarEvent(
                        task.getTaskName(),
                        task.getProjectName(),
                        date,
                        "task",
                        task.getStatus().equals("completed")
                ));
            }
        }

        eventsAdapter.notifyDataSetChanged();

        if (eventsList.isEmpty()) {
            emptyView.setVisibility(android.view.View.VISIBLE);
            eventsRecyclerView.setVisibility(android.view.View.GONE);
            emptyView.setText("No events on this date");
        } else {
            emptyView.setVisibility(android.view.View.GONE);
            eventsRecyclerView.setVisibility(android.view.View.VISIBLE);
        }
    }

    // Inner class for Calendar Events
    public static class CalendarEvent {
        private String title;
        private String subtitle;
        private String date;
        private String type;
        private boolean isCompleted;

        public CalendarEvent(String title, String subtitle, String date, String type, boolean isCompleted) {
            this.title = title;
            this.subtitle = subtitle;
            this.date = date;
            this.type = type;
            this.isCompleted = isCompleted;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getDate() { return date; }
        public String getType() { return type; }
        public boolean isCompleted() { return isCompleted; }
    }

    // Simple adapter for calendar events
    private static class CalendarEventsAdapter extends RecyclerView.Adapter<CalendarEventsAdapter.EventViewHolder> {
        private List<CalendarEvent> events;

        public CalendarEventsAdapter(List<CalendarEvent> events) {
            this.events = events;
        }

        @NonNull
        @Override
        public EventViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calendar_event, parent, false);
            return new EventViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
            CalendarEvent event = events.get(position);
            holder.titleText.setText(event.getTitle());
            holder.subtitleText.setText(event.getSubtitle());

            if (event.getType().equals("project")) {
                holder.typeIndicator.setBackgroundColor(Color.parseColor("#2196F3"));
                holder.typeText.setText("PROJECT");
            } else {
                holder.typeIndicator.setBackgroundColor(Color.parseColor("#4CAF50"));
                holder.typeText.setText("TASK");
            }

            if (event.isCompleted()) {
                holder.titleText.setAlpha(0.6f);
                holder.subtitleText.setAlpha(0.6f);
                holder.statusText.setText("✓ Completed");
                holder.statusText.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                holder.titleText.setAlpha(1.0f);
                holder.subtitleText.setAlpha(1.0f);
                holder.statusText.setText("⏱ Pending");
                holder.statusText.setTextColor(Color.parseColor("#FF9800"));
            }
        }

        @Override
        public int getItemCount() {
            return events.size();
        }

        static class EventViewHolder extends RecyclerView.ViewHolder {
            TextView titleText, subtitleText, typeText, statusText;
            android.view.View typeIndicator;

            public EventViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                titleText = itemView.findViewById(R.id.eventTitleText);
                subtitleText = itemView.findViewById(R.id.eventSubtitleText);
                typeText = itemView.findViewById(R.id.eventTypeText);
                statusText = itemView.findViewById(R.id.eventStatusText);
                typeIndicator = itemView.findViewById(R.id.typeIndicator);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjectsAndTasks();
    }
}