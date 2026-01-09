package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.request.WebhookRequest;
import com.chnindia.eighteenpluspdf.dto.response.WebhookResponse;
import com.chnindia.eighteenpluspdf.model.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service for webhook management and delivery.
 * Handles webhook registration, delivery, retries, and event notifications.
 */
@Service
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private final Map<String, WebhookRegistration> webhooks = new ConcurrentHashMap<>();
    private final Map<String, List<WebhookResponse.DeliveryAttempt>> deliveryHistory = new ConcurrentHashMap<>();
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    
    @Autowired
    public WebhookService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(4);
    }
    
    /**
     * Register a new webhook.
     */
    public WebhookResponse registerWebhook(WebhookRequest request) {
        logger.info("Registering webhook for URL: {}", request.getUrl());
        
        String webhookId = UUID.randomUUID().toString();
        
        WebhookRegistration registration = new WebhookRegistration();
        registration.setId(webhookId);
        registration.setUrl(request.getUrl());
        registration.setEvents(request.getEvents());
        registration.setSecret(request.getSecret());
        registration.setHeaders(request.getHeaders());
        registration.setActive(request.isActive());
        registration.setMaxRetries(request.getMaxRetries());
        registration.setRetryDelaySeconds(request.getRetryDelaySeconds());
        registration.setTimeoutSeconds(request.getTimeoutSeconds());
        registration.setContentType(request.getContentType());
        registration.setDescription(request.getDescription());
        registration.setFilterJobIds(request.getFilterJobIds());
        registration.setFilterToolNames(request.getFilterToolNames());
        registration.setCreatedAt(LocalDateTime.now());
        
        webhooks.put(webhookId, registration);
        deliveryHistory.put(webhookId, new ArrayList<>());
        
        return buildWebhookResponse(registration);
    }
    
    /**
     * Unregister a webhook.
     */
    public boolean unregisterWebhook(String webhookId) {
        logger.info("Unregistering webhook: {}", webhookId);
        
        WebhookRegistration removed = webhooks.remove(webhookId);
        deliveryHistory.remove(webhookId);
        
        return removed != null;
    }
    
    /**
     * Get webhook details.
     */
    public WebhookResponse getWebhook(String webhookId) {
        WebhookRegistration registration = webhooks.get(webhookId);
        if (registration == null) {
            return null;
        }
        return buildWebhookResponse(registration);
    }
    
    /**
     * List all registered webhooks.
     */
    public List<WebhookResponse> listWebhooks() {
        return webhooks.values().stream()
            .map(this::buildWebhookResponse)
            .toList();
    }
    
    /**
     * Update webhook status.
     */
    public WebhookResponse updateWebhookStatus(String webhookId, boolean active) {
        WebhookRegistration registration = webhooks.get(webhookId);
        if (registration == null) {
            return null;
        }
        
        registration.setActive(active);
        return buildWebhookResponse(registration);
    }
    
    /**
     * Trigger webhooks for a specific event.
     */
    @Async
    public void triggerWebhooks(WebhookRequest.EventType event, Map<String, Object> payload) {
        logger.debug("Triggering webhooks for event: {}", event);
        
        String jobId = payload.get("jobId") != null ? payload.get("jobId").toString() : null;
        String toolName = payload.get("toolName") != null ? payload.get("toolName").toString() : null;
        
        for (WebhookRegistration registration : webhooks.values()) {
            if (!registration.isActive()) {
                continue;
            }
            
            if (!registration.getEvents().contains(event)) {
                continue;
            }
            
            // Check job ID filter
            if (registration.getFilterJobIds() != null && !registration.getFilterJobIds().isEmpty()) {
                if (jobId == null || !registration.getFilterJobIds().contains(jobId)) {
                    continue;
                }
            }
            
            // Check tool name filter
            if (registration.getFilterToolNames() != null && !registration.getFilterToolNames().isEmpty()) {
                if (toolName == null || !registration.getFilterToolNames().contains(toolName)) {
                    continue;
                }
            }
            
            // Deliver webhook
            deliverWebhook(registration, event, payload, 1);
        }
    }
    
    /**
     * Trigger webhooks for job status change.
     */
    public void notifyJobStatusChange(JobStatus jobStatus) {
        WebhookRequest.EventType event;
        
        switch (jobStatus.getStatus()) {
            case PENDING:
                event = WebhookRequest.EventType.JOB_CREATED;
                break;
            case PROCESSING:
                event = WebhookRequest.EventType.JOB_STARTED;
                break;
            case COMPLETED:
                event = WebhookRequest.EventType.JOB_COMPLETED;
                break;
            case FAILED:
                event = WebhookRequest.EventType.JOB_FAILED;
                break;
            case CANCELLED:
                event = WebhookRequest.EventType.JOB_CANCELLED;
                break;
            default:
                return;
        }
        
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", jobStatus.getId());
        payload.put("toolName", jobStatus.getToolName());
        payload.put("status", jobStatus.getStatus().name());
        payload.put("progress", jobStatus.getProgress());
        payload.put("currentOperation", jobStatus.getCurrentOperation());
        payload.put("timestamp", LocalDateTime.now().toString());
        
        if (jobStatus.getResultUrl() != null) {
            payload.put("resultUrl", jobStatus.getResultUrl());
        }
        
        if (jobStatus.getErrorMessage() != null) {
            payload.put("error", jobStatus.getErrorMessage());
        }
        
        triggerWebhooks(event, payload);
    }
    
    /**
     * Trigger progress update webhook.
     */
    public void notifyProgress(String jobId, int progress, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jobId", jobId);
        payload.put("progress", progress);
        payload.put("message", message);
        payload.put("timestamp", LocalDateTime.now().toString());
        
        triggerWebhooks(WebhookRequest.EventType.JOB_PROGRESS, payload);
    }
    
    private void deliverWebhook(WebhookRegistration registration, WebhookRequest.EventType event, 
                                 Map<String, Object> payload, int attemptNumber) {
        long startTime = System.currentTimeMillis();
        String deliveryId = UUID.randomUUID().toString();
        
        WebhookResponse.DeliveryAttempt attempt = new WebhookResponse.DeliveryAttempt();
        attempt.setDeliveryId(deliveryId);
        attempt.setEvent(event.name());
        attempt.setTimestamp(LocalDateTime.now());
        attempt.setAttemptNumber(attemptNumber);
        attempt.setPayload(payload);
        
        try {
            // Build request body
            String body = objectMapper.writeValueAsString(buildWebhookPayload(event, payload));
            
            // Build headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(getMediaType(registration.getContentType()));
            
            // Add custom headers
            if (registration.getHeaders() != null) {
                registration.getHeaders().forEach(headers::set);
            }
            
            // Add signature if secret is configured
            if (registration.getSecret() != null && !registration.getSecret().isEmpty()) {
                String signature = computeSignature(body, registration.getSecret());
                headers.set("X-Webhook-Signature", signature);
            }
            
            headers.set("X-Webhook-Id", registration.getId());
            headers.set("X-Delivery-Id", deliveryId);
            headers.set("X-Event-Type", event.name());
            
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            
            // Send request
            ResponseEntity<String> response = restTemplate.exchange(
                URI.create(registration.getUrl()),
                HttpMethod.POST,
                request,
                String.class
            );
            
            long latency = System.currentTimeMillis() - startTime;
            
            attempt.setSuccess(response.getStatusCode().is2xxSuccessful());
            attempt.setResponseCode(response.getStatusCode().value());
            attempt.setResponseBody(truncate(response.getBody(), 1000));
            attempt.setLatencyMs(latency);
            
            registration.setLastTriggeredAt(LocalDateTime.now());
            registration.incrementDeliveries(attempt.isSuccess());
            
            logger.info("Webhook delivered successfully: {} -> {} ({}ms)", 
                event, registration.getUrl(), latency);
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            
            attempt.setSuccess(false);
            attempt.setResponseCode(0);
            attempt.setResponseBody("Error: " + e.getMessage());
            attempt.setLatencyMs(latency);
            
            registration.incrementDeliveries(false);
            
            logger.warn("Webhook delivery failed: {} -> {} - {}", 
                event, registration.getUrl(), e.getMessage());
            
            // Schedule retry
            if (attemptNumber < registration.getMaxRetries()) {
                long delayMs = registration.getRetryDelaySeconds() * 1000L * attemptNumber;
                scheduler.schedule(
                    () -> deliverWebhook(registration, event, payload, attemptNumber + 1),
                    delayMs,
                    TimeUnit.MILLISECONDS
                );
            }
        }
        
        // Store delivery attempt
        List<WebhookResponse.DeliveryAttempt> history = deliveryHistory.get(registration.getId());
        if (history != null) {
            history.add(attempt);
            // Keep only last 100 deliveries
            while (history.size() > 100) {
                history.remove(0);
            }
        }
    }
    
    private Map<String, Object> buildWebhookPayload(WebhookRequest.EventType event, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event.name());
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("data", data);
        return payload;
    }
    
    private MediaType getMediaType(WebhookRequest.ContentType contentType) {
        if (contentType == null) {
            return MediaType.APPLICATION_JSON;
        }
        
        switch (contentType) {
            case FORM_URLENCODED:
                return MediaType.APPLICATION_FORM_URLENCODED;
            case XML:
                return MediaType.APPLICATION_XML;
            case JSON:
            default:
                return MediaType.APPLICATION_JSON;
        }
    }
    
    private String computeSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + bytesToHex(hash);
        } catch (Exception e) {
            logger.error("Failed to compute webhook signature", e);
            return "";
        }
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
    
    private WebhookResponse buildWebhookResponse(WebhookRegistration registration) {
        WebhookResponse response = new WebhookResponse();
        response.setWebhookId(registration.getId());
        response.setUrl(registration.getUrl());
        response.setEvents(registration.getEvents().stream().map(Enum::name).toList());
        response.setActive(registration.isActive());
        response.setCreatedAt(registration.getCreatedAt());
        response.setLastTriggeredAt(registration.getLastTriggeredAt());
        
        WebhookResponse.WebhookStatistics stats = new WebhookResponse.WebhookStatistics();
        stats.setTotalDeliveries(registration.getTotalDeliveries());
        stats.setSuccessfulDeliveries(registration.getSuccessfulDeliveries());
        stats.setFailedDeliveries(registration.getFailedDeliveries());
        stats.setSuccessRate(registration.getTotalDeliveries() > 0 ? 
            (double) registration.getSuccessfulDeliveries() / registration.getTotalDeliveries() * 100 : 0);
        response.setStatistics(stats);
        
        List<WebhookResponse.DeliveryAttempt> history = deliveryHistory.get(registration.getId());
        if (history != null) {
            int start = Math.max(0, history.size() - 10);
            response.setRecentDeliveries(new ArrayList<>(history.subList(start, history.size())));
        }
        
        return response;
    }
    
    /**
     * Internal class for webhook registration data.
     */
    private static class WebhookRegistration {
        private String id;
        private String url;
        private List<WebhookRequest.EventType> events;
        private String secret;
        private Map<String, String> headers;
        private boolean active;
        private int maxRetries;
        private int retryDelaySeconds;
        private int timeoutSeconds;
        private WebhookRequest.ContentType contentType;
        private String description;
        private List<String> filterJobIds;
        private List<String> filterToolNames;
        private LocalDateTime createdAt;
        private LocalDateTime lastTriggeredAt;
        private long totalDeliveries = 0;
        private long successfulDeliveries = 0;
        private long failedDeliveries = 0;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public List<WebhookRequest.EventType> getEvents() { return events; }
        public void setEvents(List<WebhookRequest.EventType> events) { this.events = events; }
        
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public int getRetryDelaySeconds() { return retryDelaySeconds; }
        public void setRetryDelaySeconds(int retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }
        
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
        
        public WebhookRequest.ContentType getContentType() { return contentType; }
        public void setContentType(WebhookRequest.ContentType contentType) { this.contentType = contentType; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public List<String> getFilterJobIds() { return filterJobIds; }
        public void setFilterJobIds(List<String> filterJobIds) { this.filterJobIds = filterJobIds; }
        
        public List<String> getFilterToolNames() { return filterToolNames; }
        public void setFilterToolNames(List<String> filterToolNames) { this.filterToolNames = filterToolNames; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
        public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
        
        public long getTotalDeliveries() { return totalDeliveries; }
        public long getSuccessfulDeliveries() { return successfulDeliveries; }
        public long getFailedDeliveries() { return failedDeliveries; }
        
        public synchronized void incrementDeliveries(boolean success) {
            totalDeliveries++;
            if (success) {
                successfulDeliveries++;
            } else {
                failedDeliveries++;
            }
        }
    }
}
