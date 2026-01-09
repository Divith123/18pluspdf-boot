package com.chnindia.eighteenpluspdf.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for webhook registration and management.
 */
public class WebhookResponse {
    
    private String webhookId;
    private String url;
    private List<String> events;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastTriggeredAt;
    private WebhookStatistics statistics;
    private List<DeliveryAttempt> recentDeliveries;
    
    public static class WebhookStatistics {
        private long totalDeliveries;
        private long successfulDeliveries;
        private long failedDeliveries;
        private double successRate;
        private double averageLatencyMs;
        
        // Getters and Setters
        public long getTotalDeliveries() { return totalDeliveries; }
        public void setTotalDeliveries(long totalDeliveries) { this.totalDeliveries = totalDeliveries; }
        
        public long getSuccessfulDeliveries() { return successfulDeliveries; }
        public void setSuccessfulDeliveries(long successfulDeliveries) { this.successfulDeliveries = successfulDeliveries; }
        
        public long getFailedDeliveries() { return failedDeliveries; }
        public void setFailedDeliveries(long failedDeliveries) { this.failedDeliveries = failedDeliveries; }
        
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        
        public double getAverageLatencyMs() { return averageLatencyMs; }
        public void setAverageLatencyMs(double averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }
    }
    
    public static class DeliveryAttempt {
        private String deliveryId;
        private String event;
        private LocalDateTime timestamp;
        private boolean success;
        private int responseCode;
        private String responseBody;
        private int attemptNumber;
        private long latencyMs;
        private Map<String, Object> payload;
        
        // Getters and Setters
        public String getDeliveryId() { return deliveryId; }
        public void setDeliveryId(String deliveryId) { this.deliveryId = deliveryId; }
        
        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getResponseCode() { return responseCode; }
        public void setResponseCode(int responseCode) { this.responseCode = responseCode; }
        
        public String getResponseBody() { return responseBody; }
        public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
        
        public int getAttemptNumber() { return attemptNumber; }
        public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
        
        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
        
        public Map<String, Object> getPayload() { return payload; }
        public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    }
    
    // Main class Getters and Setters
    
    public String getWebhookId() { return webhookId; }
    public void setWebhookId(String webhookId) { this.webhookId = webhookId; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
    
    public WebhookStatistics getStatistics() { return statistics; }
    public void setStatistics(WebhookStatistics statistics) { this.statistics = statistics; }
    
    public List<DeliveryAttempt> getRecentDeliveries() { return recentDeliveries; }
    public void setRecentDeliveries(List<DeliveryAttempt> recentDeliveries) { this.recentDeliveries = recentDeliveries; }
}
