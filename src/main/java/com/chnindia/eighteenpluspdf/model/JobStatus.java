package com.chnindia.eighteenpluspdf.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "jobs")
public class JobStatus {
    @Id
    private String id;
    
    @Enumerated(EnumType.STRING)
    private Status status;
    
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
    private String resultHash;
    private Long fileSize;

    public enum Status {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    public JobStatus() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.status = Status.PENDING;
        this.progress = 0;
    }

    public JobStatus(String id, String toolName, String fileName) {
        this();
        this.id = id;
        this.toolName = toolName;
        this.fileName = fileName;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { 
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

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
    public void setProgress(Integer progress) { 
        this.progress = progress;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCurrentOperation() { return currentOperation; }
    public void setCurrentOperation(String currentOperation) { 
        this.currentOperation = currentOperation;
        this.updatedAt = LocalDateTime.now();
    }

    public String getResultHash() { return resultHash; }
    public void setResultHash(String resultHash) { this.resultHash = resultHash; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}