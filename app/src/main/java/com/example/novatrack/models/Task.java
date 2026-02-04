package com.example.novatrack.models;

public class Task {
    private String taskId;
    private String projectId;
    private String projectName;
    private String taskName;
    private String taskDescription;
    private String deadline;
    private String status;
    private String priority;
    private long createdAt;
    private long completedAt;
    private String userId;

    public Task() {

    }

    public Task(String projectId, String projectName, String taskName, String taskDescription,
                String deadline, String status, String priority, long createdAt, String userId) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.taskName = taskName;
        this.taskDescription = taskDescription;
        this.deadline = deadline;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.completedAt = 0;
        this.userId = userId;
    }


    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}