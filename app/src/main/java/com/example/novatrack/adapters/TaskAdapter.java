package com.example.novatrack.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.novatrack.R;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CheckBox taskCheckbox;
        TextView taskNameText, projectNameText, deadlineText, priorityBadge;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            taskNameText = itemView.findViewById(R.id.taskNameText);
            projectNameText = itemView.findViewById(R.id.projectNameText);
            deadlineText = itemView.findViewById(R.id.deadlineText);
            priorityBadge = itemView.findViewById(R.id.priorityBadge);
        }

        public void bind(Task task, OnTaskClickListener listener) {
            taskNameText.setText(task.getTaskName());
            projectNameText.setText(task.getProjectName());
            deadlineText.setText("Due: " + task.getDeadline());
            priorityBadge.setText(task.getPriority().toUpperCase());

            // Set checkbox state
            boolean isCompleted = task.getStatus().equals("completed");
            taskCheckbox.setChecked(isCompleted);

            // Apply strikethrough if completed
            if (isCompleted) {
                taskNameText.setPaintFlags(taskNameText.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                taskNameText.setAlpha(0.6f);
                projectNameText.setAlpha(0.6f);
                deadlineText.setAlpha(0.6f);
            } else {
                taskNameText.setPaintFlags(taskNameText.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                taskNameText.setAlpha(1.0f);
                projectNameText.setAlpha(1.0f);
                deadlineText.setAlpha(1.0f);
            }

            // Set priority badge color
            switch (task.getPriority().toLowerCase()) {
                case "high":
                    priorityBadge.setBackgroundResource(R.drawable.bg_priority_high);
                    break;
                case "medium":
                    priorityBadge.setBackgroundResource(R.drawable.bg_priority_medium);
                    break;
                case "low":
                    priorityBadge.setBackgroundResource(R.drawable.bg_priority_low);
                    break;
            }

            // Check if task is overdue
            if (!isCompleted && isOverdue(task.getDeadline())) {
                deadlineText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
                deadlineText.setText("⚠️ Overdue: " + task.getDeadline());
            } else {
                deadlineText.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray));
            }

            // Checkbox listener
            taskCheckbox.setOnClickListener(v -> {
                listener.onTaskChecked(task, taskCheckbox.isChecked());
            });

            // Long click listener for edit/delete
            itemView.setOnLongClickListener(v -> {
                listener.onTaskLongClick(task);
                return true;
            });
        }

        private boolean isOverdue(String deadline) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date dueDate = sdf.parse(deadline);
                Date today = new Date();
                return dueDate != null && dueDate.before(today);
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}