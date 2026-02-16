package com.example.novatrack.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.novatrack.ProjectDetailsActivity;
import com.example.novatrack.R;
import com.example.novatrack.models.Project;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {

    private List<Project> projectList;
    private OnProjectLongClickListener longClickListener;

    public interface OnProjectLongClickListener {
        void onProjectLongClick(Project project, View view);
    }

    public ProjectAdapter(List<Project> projectList, OnProjectLongClickListener longClickListener) {
        this.projectList = projectList;
        this.longClickListener = longClickListener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_project, parent, false);
        return new ProjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projectList.get(position);
        Context context = holder.itemView.getContext();

        holder.titleText.setText(project.getTitle());
        holder.statusText.setText(project.getStatus());
        holder.progressText.setText(project.getProgress() + "%");
        holder.progressBar.setProgress(project.getProgress());

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.onProjectLongClick(project, v);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProjectDetailsActivity.class);
            intent.putExtra("PROJECT_ID", project.getId());
            intent.putExtra("PROJECT_TITLE", project.getTitle());
            intent.putExtra("PROJECT_DESCRIPTION", project.getDescription());
            intent.putExtra("PROJECT_SUBJECT", project.getSubject());
            intent.putExtra("PROJECT_DUE_DATE", project.getDueDate());
            intent.putExtra("PROJECT_PROGRESS", project.getProgress());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return projectList.size();
    }

    static class ProjectViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, statusText, progressText;
        ProgressBar progressBar;

        public ProjectViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.projectTitle);
            statusText = itemView.findViewById(R.id.statusText);
            progressText = itemView.findViewById(R.id.progressText);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}