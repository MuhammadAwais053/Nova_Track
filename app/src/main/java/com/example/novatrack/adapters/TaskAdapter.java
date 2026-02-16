package com.example.novatrack.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.novatrack.R;
import com.example.novatrack.TaskDetailsActivity;
import com.example.novatrack.models.Task;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskChecked(Task task, boolean isChecked);
        void onTaskLongClick(Task task);
    }

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        Context context = holder.itemView.getContext();

        holder.taskNameText.setText(task.getTaskName());
        holder.projectNameText.setText(task.getProjectName());
        holder.deadlineText.setText("Due: " + task.getDeadline());

        String priority = task.getPriority().toUpperCase();
        holder.priorityBadge.setText(priority);

        int priorityBg;
        if (task.getPriority().equals("high")) {
            priorityBg = R.drawable.bg_priority_high;
        } else if (task.getPriority().equals("medium")) {
            priorityBg = R.drawable.bg_priority_medium;
        } else {
            priorityBg = R.drawable.bg_priority_low;
        }
        holder.priorityBadge.setBackgroundResource(priorityBg);

        boolean isCompleted = task.getStatus().equals("completed");
        holder.taskCheckBox.setChecked(isCompleted);

        if (isCompleted) {
            holder.taskNameText.setPaintFlags(holder.taskNameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskNameText.setAlpha(0.6f);
        } else {
            holder.taskNameText.setPaintFlags(holder.taskNameText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.taskNameText.setAlpha(1.0f);
        }

        if (isOverdue(task.getDeadline()) && !isCompleted) {
            holder.overdueWarning.setVisibility(View.VISIBLE);
        } else {
            holder.overdueWarning.setVisibility(View.GONE);
        }

        holder.taskCheckBox.setOnClickListener(v -> listener.onTaskChecked(task, holder.taskCheckBox.isChecked()));
        holder.taskCard.setOnLongClickListener(v -> {
            listener.onTaskLongClick(task);
            return true;
        });
        holder.taskCard.setOnClickListener(v -> {
            Intent intent = new Intent(context, TaskDetailsActivity.class);
            intent.putExtra("TASK_NAME", task.getTaskName());
            intent.putExtra("PROJECT_NAME", task.getProjectName());
            intent.putExtra("DESCRIPTION", task.getTaskDescription());
            intent.putExtra("DEADLINE", task.getDeadline());
            intent.putExtra("PRIORITY", task.getPriority());
            intent.putExtra("STATUS", task.getStatus());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private boolean isOverdue(String deadline) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date taskDate = sdf.parse(deadline);
            Date today = new Date();
            return taskDate != null && taskDate.before(today);
        } catch (ParseException e) {
            return false;
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CardView taskCard;
        CheckBox taskCheckBox;
        TextView taskNameText, projectNameText, deadlineText, priorityBadge, overdueWarning;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCard = itemView.findViewById(R.id.taskCard);
            taskCheckBox = itemView.findViewById(R.id.taskCheckBox);
            taskNameText = itemView.findViewById(R.id.taskNameText);
            projectNameText = itemView.findViewById(R.id.projectNameText);
            deadlineText = itemView.findViewById(R.id.deadlineText);
            priorityBadge = itemView.findViewById(R.id.priorityBadge);
            overdueWarning = itemView.findViewById(R.id.overdueWarning);
        }
    }
}