package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for webhook registration and management.
 * Supports job completion callbacks, progress updates, and event notifications.
 */
public class WebhookRequest {
    
    @NotBlank(message = "Webhook URL is required")
    private String url;
    
    @NotNull(message = "At least one event type is required")
    private List<EventType> events;
    
    private String secret; // For HMAC signature verification
    
    private Map<String, String> headers; // Custom headers to send
    
    private boolean active = true;
    
    private int maxRetries = 3;
    
    private int retryDelaySeconds = 30;
    
    private int timeoutSeconds = 30;
    
    private ContentType contentType = ContentType.JSON;
    
    private String description;
    
    private List<String> filterJobIds; // Only send for specific jobs
    
    private List<String> filterToolNames; // Only send for specific tools
    
    public enum EventType {
        JOB_CREATED,
        JOB_STARTED,
        JOB_PROGRESS,
        JOB_COMPLETED,
        JOB_FAILED,
        JOB_CANCELLED,
        FILE_READY,
        FILE_EXPIRED,
        QUOTA_WARNING,
        QUOTA_EXCEEDED,
        BATCH_COMPLETED
    }
    
    public enum ContentType {
        JSON,
        FORM_URLENCODED,
        XML
    }
    
    // Getters and Setters
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public List<EventType> getEvents() {
        return events;
    }
    
    public void setEvents(List<EventType> events) {
        this.events = events;
    }
    
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public Map<String, String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public int getMaxRetries() {
        return maxRetries;
    }
    
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
    
    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }
    
    public void setRetryDelaySeconds(int retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }
    
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
    
    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    public ContentType getContentType() {
        return contentType;
    }
    
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getFilterJobIds() {
        return filterJobIds;
    }
    
    public void setFilterJobIds(List<String> filterJobIds) {
        this.filterJobIds = filterJobIds;
    }
    
    public List<String> getFilterToolNames() {
        return filterToolNames;
    }
    
    public void setFilterToolNames(List<String> filterToolNames) {
        this.filterToolNames = filterToolNames;
    }
}
