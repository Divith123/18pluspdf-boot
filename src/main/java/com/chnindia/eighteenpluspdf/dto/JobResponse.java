package com.chnindia.eighteenpluspdf.dto;

import java.time.LocalDateTime;

import com.chnindia.eighteenpluspdf.model.JobStatus;

public class JobResponse {
    private String jobId;
    private JobStatus.Status status;
    private String toolName;
    private String fileName;
    private String resultUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long processingTimeMs;

    public JobResponse() {}

    public JobResponse(JobStatus jobStatus) {
        this.jobId = jobStatus.getId();
        this.status = jobStatus.getStatus();
        this.toolName = jobStatus.getToolName();
        this.fileName = jobStatus.getFileName();
        this.resultUrl = jobStatus.getResultUrl();
        this.errorMessage = jobStatus.getErrorMessage();
        this.createdAt = jobStatus.getCreatedAt();
        this.completedAt = jobStatus.getCompletedAt();
        this.processingTimeMs = jobStatus.getProcessingTimeMs();
    }

    // Getters and Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public JobStatus.Status getStatus() { return status; }
    public void setStatus(JobStatus.Status status) { this.status = status; }

    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getResultUrl() { return resultUrl; }
    public void setResultUrl(String resultUrl) { this.resultUrl = resultUrl; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}