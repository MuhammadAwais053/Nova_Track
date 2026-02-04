package com.example.novatrack;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.novatrack.models.Project;
import com.example.novatrack.utils.StatusBarHelper;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProgressActivity extends AppCompatActivity {

    private PieChart pieChart;
    private BarChart barChart;
    private TextView totalProjectsText, completedProjectsText, inProgressProjectsText, overallProgressText;
    private TextView emptyView;
    private ImageView backButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Project> projectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        StatusBarHelper.setTransparentStatusBar(this, true);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        projectList = new ArrayList<>();

        pieChart = findViewById(R.id.pieChart);
        barChart = findViewById(R.id.barChart);
        totalProjectsText = findViewById(R.id.totalProjectsText);
        completedProjectsText = findViewById(R.id.completedProjectsText);
        inProgressProjectsText = findViewById(R.id.inProgressProjectsText);
        overallProgressText = findViewById(R.id.overallProgressText);
        emptyView = findViewById(R.id.emptyView);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        loadProjectsData();
    }

    private void loadProjectsData() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("projects")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    projectList.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        showEmptyView();
                        return;
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Project project = doc.toObject(Project.class);
                        project.setId(doc.getId());
                        projectList.add(project);
                    }

                    hideEmptyView();
                    calculateStatistics();
                    setupPieChart();
                    setupBarChart();
                })
                .addOnFailureListener(e -> {
                    showEmptyView();
                });
    }

    private void calculateStatistics() {
        int totalProjects = projectList.size();
        int completedProjects = 0;
        int inProgressProjects = 0;
        int totalProgress = 0;

        for (Project project : projectList) {
            totalProgress += project.getProgress();
            if (project.getStatus().equals("Completed")) {
                completedProjects++;
            } else {
                inProgressProjects++;
            }
        }

        int overallProgress = totalProjects > 0 ? totalProgress / totalProjects : 0;

        totalProjectsText.setText(String.valueOf(totalProjects));
        completedProjectsText.setText(String.valueOf(completedProjects));
        inProgressProjectsText.setText(String.valueOf(inProgressProjects));
        overallProgressText.setText(overallProgress + "% Overall");
    }

    private void setupPieChart() {
        int completed = 0;
        int inProgress = 0;

        for (Project project : projectList) {
            if (project.getStatus().equals("Completed")) {
                completed++;
            } else {
                inProgress++;
            }
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(completed, "Completed"));
        entries.add(new PieEntry(inProgress, "In Progress"));

        PieDataSet dataSet = new PieDataSet(entries, "Project Status");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleRadius(58f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    private void setupBarChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        for (int i = 0; i < projectList.size(); i++) {
            Project project = projectList.get(i);
            entries.add(new BarEntry(i, project.getProgress()));

            // Truncate long project names
            String label = project.getTitle();
            if (label.length() > 10) {
                label = label.substring(0, 10) + "...";
            }
            labels.add(label);
        }

        BarDataSet dataSet = new BarDataSet(entries, "Project Progress (%)");
        dataSet.setColors(ColorTemplate.COLORFUL_COLORS);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        barChart.setData(data);
        barChart.getDescription().setEnabled(false);
        barChart.setFitBars(true);
        barChart.animateY(1000);

        // X-axis configuration
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setLabelRotationAngle(-45);

        // Y-axis configuration
        barChart.getAxisLeft().setAxisMinimum(0f);
        barChart.getAxisLeft().setAxisMaximum(100f);
        barChart.getAxisRight().setEnabled(false);

        barChart.invalidate();
    }

    private void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        pieChart.setVisibility(View.GONE);
        barChart.setVisibility(View.GONE);
        totalProjectsText.setText("0");
        completedProjectsText.setText("0");
        inProgressProjectsText.setText("0");
        overallProgressText.setText("0% Overall");
    }

    private void hideEmptyView() {
        emptyView.setVisibility(View.GONE);
        pieChart.setVisibility(View.VISIBLE);
        barChart.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProjectsData();
    }
}