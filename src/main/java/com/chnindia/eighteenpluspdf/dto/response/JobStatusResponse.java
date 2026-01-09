package com.chnindia.eighteenpluspdf.dto.response;

import com.chnindia.eighteenpluspdf.model.JobStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for job status information
 */
public class JobStatusResponse {
    
    private String jobId;
    private JobStatus.Status status;
    private String toolName;
    private String fileName;
    private String resultUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Long processingTimeMs;
    private Integer progress;
    private String currentOperation;
    
    public JobStatusResponse() {}
    
    public JobStatusResponse(JobStatus jobStatus) {
        this.jobId = jobStatus.getId();
        this.status = jobStatus.getStatus();
        this.toolName = jobStatus.getToolName();
        this.fileName = jobStatus.getFileName();
        this.resultUrl = jobStatus.getResultUrl();
        this.errorMessage = jobStatus.getErrorMessage();
        this.createdAt = jobStatus.getCreatedAt();
        this.updatedAt = jobStatus.getUpdatedAt();
        this.completedAt = jobStatus.getCompletedAt();
        this.processingTimeMs = jobStatus.getProcessingTimeMs();
        this.progress = jobStatus.getProgress();
        this.currentOperation = jobStatus.getCurrentOperation();
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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public String getCurrentOperation() { return currentOperation; }
    public void setCurrentOperation(String currentOperation) { this.currentOperation = currentOperation; }
}