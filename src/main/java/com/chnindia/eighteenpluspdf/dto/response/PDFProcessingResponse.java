package com.chnindia.eighteenpluspdf.dto.response;

import com.chnindia.eighteenpluspdf.dto.JobResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic response DTO for PDF processing operations
 */
public class PDFProcessingResponse {
    
    private Boolean success;
    private String jobId;
    private String status;
    private String message;
    private String downloadUrl;
    private List<String> outputFiles;
    private Long processingTimeMs;
    private LocalDateTime timestamp;
    private Object resultData;
    
    public PDFProcessingResponse() {}
    
    public PDFProcessingResponse(Boolean success, String message) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public PDFProcessingResponse(JobResponse jobResponse) {
        this.success = true;
        this.jobId = jobResponse.getJobId();
        this.status = jobResponse.getStatus() != null ? jobResponse.getStatus().name() : "PENDING";
        this.message = jobResponse.getErrorMessage() != null ? jobResponse.getErrorMessage() : "Job accepted";
        this.downloadUrl = jobResponse.getResultUrl();
        this.timestamp = LocalDateTime.now();
    }
    
    public static PDFProcessingResponse success(String jobId, String downloadUrl) {
        PDFProcessingResponse response = new PDFProcessingResponse(true, "Processing completed successfully");
        response.setJobId(jobId);
        response.setDownloadUrl(downloadUrl);
        response.setStatus("COMPLETED");
        return response;
    }
    
    public static PDFProcessingResponse successWithFiles(String jobId, List<String> files) {
        PDFProcessingResponse response = new PDFProcessingResponse(true, "Processing completed successfully");
        response.setJobId(jobId);
        response.setOutputFiles(files);
        response.setStatus("COMPLETED");
        return response;
    }
    
    public static PDFProcessingResponse error(String message) {
        return new PDFProcessingResponse(false, message);
    }
    
    public static PDFProcessingResponse queued(String jobId) {
        PDFProcessingResponse response = new PDFProcessingResponse(true, "Job queued successfully");
        response.setJobId(jobId);
        response.setStatus("QUEUED");
        return response;
    }
    
    public static PDFProcessingResponse fromJobResponse(JobResponse jobResponse) {
        return new PDFProcessingResponse(jobResponse);
    }

    // Getters and Setters
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public List<String> getOutputFiles() { return outputFiles; }
    public void setOutputFiles(List<String> outputFiles) { this.outputFiles = outputFiles; }

    public Long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Long processingTimeMs) { this.processingTimeMs = processingTimeMs; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Object getResultData() { return resultData; }
    public void setResultData(Object resultData) { this.resultData = resultData; }
}