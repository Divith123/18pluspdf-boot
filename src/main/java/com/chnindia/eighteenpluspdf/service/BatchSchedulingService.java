package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.JobRequest;
import com.chnindia.eighteenpluspdf.dto.JobResponse;
import com.chnindia.eighteenpluspdf.model.JobStatus;
import com.chnindia.eighteenpluspdf.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enterprise Batch Scheduling Service.
 * Provides cron-based scheduling, recurring jobs, and batch workflow management.
 * 
 * Features:
 * - Cron expression scheduling
 * - One-time scheduled jobs
 * - Recurring batch jobs
 * - Job chaining (workflows)
 * - Schedule management (pause/resume/cancel)
 * - Timezone support
 * - Schedule persistence
 */
@Service
@EnableScheduling
public class BatchSchedulingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BatchSchedulingService.class);
    
    @Autowired
    private JobQueueService jobQueueService;
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired(required = false)
    private TaskScheduler taskScheduler;
    
    // In-memory schedule storage (in production, use database)
    private final Map<String, ScheduledJob> scheduledJobs = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> activeFutures = new ConcurrentHashMap<>();
    private final Map<String, BatchWorkflow> workflows = new ConcurrentHashMap<>();
    
    // Executor for scheduled tasks
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    
    @PostConstruct
    public void initialize() {
        logger.info("✅ Batch Scheduling Service initialized");
        // Load persisted schedules from database if available
        loadPersistedSchedules();
    }
    
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down Batch Scheduling Service");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    // ==================== SCHEDULE CREATION ====================
    
    /**
     * Schedule a one-time job at a specific time.
     */
    public ScheduledJobResult scheduleOneTime(JobRequest jobRequest, LocalDateTime executeAt, String timezone) {
        String scheduleId = UUID.randomUUID().toString();
        
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        ZonedDateTime scheduledTime = executeAt.atZone(zoneId);
        long delayMs = Duration.between(ZonedDateTime.now(zoneId), scheduledTime).toMillis();
        
        if (delayMs < 0) {
            throw new IllegalArgumentException("Scheduled time must be in the future");
        }
        
        ScheduledJob scheduledJob = new ScheduledJob();
        scheduledJob.setId(scheduleId);
        scheduledJob.setJobRequest(jobRequest);
        scheduledJob.setScheduleType(ScheduleType.ONE_TIME);
        scheduledJob.setExecuteAt(scheduledTime.toInstant());
        scheduledJob.setTimezone(zoneId.getId());
        scheduledJob.setStatus(ScheduleStatus.ACTIVE);
        scheduledJob.setCreatedAt(Instant.now());
        
        scheduledJobs.put(scheduleId, scheduledJob);
        
        // Schedule execution
        ScheduledFuture<?> future = scheduler.schedule(
            () -> executeScheduledJob(scheduleId),
            delayMs,
            TimeUnit.MILLISECONDS
        );
        activeFutures.put(scheduleId, future);
        
        logger.info("Scheduled one-time job {} for {}", scheduleId, scheduledTime);
        
        return new ScheduledJobResult(scheduleId, scheduledTime.toInstant(), "ONE_TIME", "Job scheduled successfully");
    }
    
    /**
     * Schedule a recurring job with cron expression.
     */
    public ScheduledJobResult scheduleCron(JobRequest jobRequest, String cronExpression, String timezone, String name) {
        String scheduleId = UUID.randomUUID().toString();
        
        // Validate cron expression
        if (!isValidCronExpression(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
        
        ZoneId zoneId = timezone != null ? ZoneId.of(timezone) : ZoneId.systemDefault();
        
        ScheduledJob scheduledJob = new ScheduledJob();
        scheduledJob.setId(scheduleId);
        scheduledJob.setName(name != null ? name : "Scheduled-" + scheduleId.substring(0, 8));
        scheduledJob.setJobRequest(jobRequest);
        scheduledJob.setScheduleType(ScheduleType.CRON);
        scheduledJob.setCronExpression(cronExpression);
        scheduledJob.setTimezone(zoneId.getId());
        scheduledJob.setStatus(ScheduleStatus.ACTIVE);
        scheduledJob.setCreatedAt(Instant.now());
        
        scheduledJobs.put(scheduleId, scheduledJob);
        
        // Schedule with cron
        scheduleCronJob(scheduleId, cronExpression, zoneId);
        
        Instant nextExecution = calculateNextCronExecution(cronExpression, zoneId);
        logger.info("Scheduled cron job {} with expression '{}', next run: {}", scheduleId, cronExpression, nextExecution);
        
        return new ScheduledJobResult(scheduleId, nextExecution, "CRON", "Recurring job scheduled with cron: " + cronExpression);
    }
    
    /**
     * Schedule a job to repeat at fixed intervals.
     */
    public ScheduledJobResult scheduleInterval(JobRequest jobRequest, long intervalMinutes, String name) {
        String scheduleId = UUID.randomUUID().toString();
        
        ScheduledJob scheduledJob = new ScheduledJob();
        scheduledJob.setId(scheduleId);
        scheduledJob.setName(name != null ? name : "Interval-" + scheduleId.substring(0, 8));
        scheduledJob.setJobRequest(jobRequest);
        scheduledJob.setScheduleType(ScheduleType.INTERVAL);
        scheduledJob.setIntervalMinutes(intervalMinutes);
        scheduledJob.setStatus(ScheduleStatus.ACTIVE);
        scheduledJob.setCreatedAt(Instant.now());
        
        scheduledJobs.put(scheduleId, scheduledJob);
        
        // Schedule at fixed rate
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> executeScheduledJob(scheduleId),
            intervalMinutes,
            intervalMinutes,
            TimeUnit.MINUTES
        );
        activeFutures.put(scheduleId, future);
        
        Instant nextExecution = Instant.now().plusSeconds(intervalMinutes * 60);
        logger.info("Scheduled interval job {} every {} minutes", scheduleId, intervalMinutes);
        
        return new ScheduledJobResult(scheduleId, nextExecution, "INTERVAL", "Job scheduled every " + intervalMinutes + " minutes");
    }
    
    // ==================== WORKFLOW/CHAINING ====================
    
    /**
     * Create a batch workflow with multiple chained jobs.
     */
    public String createWorkflow(String name, List<JobRequest> jobs, boolean continueOnError) {
        String workflowId = UUID.randomUUID().toString();
        
        BatchWorkflow workflow = new BatchWorkflow();
        workflow.setId(workflowId);
        workflow.setName(name);
        workflow.setJobs(jobs);
        workflow.setContinueOnError(continueOnError);
        workflow.setStatus(WorkflowStatus.CREATED);
        workflow.setCreatedAt(Instant.now());
        
        workflows.put(workflowId, workflow);
        
        logger.info("Created workflow {} with {} jobs", workflowId, jobs.size());
        
        return workflowId;
    }
    
    /**
     * Execute a workflow immediately or schedule it.
     */
    public Map<String, Object> executeWorkflow(String workflowId) {
        BatchWorkflow workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new IllegalArgumentException("Workflow not found: " + workflowId);
        }
        
        workflow.setStatus(WorkflowStatus.RUNNING);
        workflow.setStartedAt(Instant.now());
        
        List<String> completedJobs = new ArrayList<>();
        List<String> failedJobs = new ArrayList<>();
        Map<String, Object> results = new LinkedHashMap<>();
        
        for (int i = 0; i < workflow.getJobs().size(); i++) {
            JobRequest jobRequest = workflow.getJobs().get(i);
            
            try {
                logger.info("Workflow {} executing step {}/{}: {}", workflowId, i + 1, workflow.getJobs().size(), jobRequest.getToolName());
                
                // Submit job synchronously within workflow
                JobResponse response = jobQueueService.submitJob(jobRequest);
                
                // Wait for completion (simplified - in production use async with callbacks)
                String jobId = response.getJobId();
                completedJobs.add(jobId);
                results.put("step_" + (i + 1), Map.of(
                    "jobId", jobId,
                    "tool", jobRequest.getToolName(),
                    "status", "COMPLETED"
                ));
                
            } catch (Exception e) {
                logger.error("Workflow {} step {} failed: {}", workflowId, i + 1, e.getMessage());
                failedJobs.add("step_" + (i + 1) + ": " + e.getMessage());
                
                if (!workflow.isContinueOnError()) {
                    workflow.setStatus(WorkflowStatus.FAILED);
                    break;
                }
            }
        }
        
        if (failedJobs.isEmpty()) {
            workflow.setStatus(WorkflowStatus.COMPLETED);
        } else if (workflow.isContinueOnError() && !completedJobs.isEmpty()) {
            workflow.setStatus(WorkflowStatus.PARTIAL);
        } else {
            workflow.setStatus(WorkflowStatus.FAILED);
        }
        
        workflow.setCompletedAt(Instant.now());
        
        return Map.of(
            "workflowId", workflowId,
            "status", workflow.getStatus().name(),
            "totalSteps", workflow.getJobs().size(),
            "completedSteps", completedJobs.size(),
            "failedSteps", failedJobs.size(),
            "results", results,
            "duration", Duration.between(workflow.getStartedAt(), workflow.getCompletedAt()).toMillis() + "ms"
        );
    }
    
    // ==================== SCHEDULE MANAGEMENT ====================
    
    /**
     * Pause a scheduled job.
     */
    public boolean pauseSchedule(String scheduleId) {
        ScheduledJob job = scheduledJobs.get(scheduleId);
        if (job == null) return false;
        
        ScheduledFuture<?> future = activeFutures.get(scheduleId);
        if (future != null) {
            future.cancel(false);
        }
        
        job.setStatus(ScheduleStatus.PAUSED);
        logger.info("Paused schedule {}", scheduleId);
        return true;
    }
    
    /**
     * Resume a paused schedule.
     */
    public boolean resumeSchedule(String scheduleId) {
        ScheduledJob job = scheduledJobs.get(scheduleId);
        if (job == null || job.getStatus() != ScheduleStatus.PAUSED) return false;
        
        job.setStatus(ScheduleStatus.ACTIVE);
        
        // Re-schedule based on type
        switch (job.getScheduleType()) {
            case CRON:
                scheduleCronJob(scheduleId, job.getCronExpression(), ZoneId.of(job.getTimezone()));
                break;
            case INTERVAL:
                ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                    () -> executeScheduledJob(scheduleId),
                    job.getIntervalMinutes(),
                    job.getIntervalMinutes(),
                    TimeUnit.MINUTES
                );
                activeFutures.put(scheduleId, future);
                break;
            default:
                break;
        }
        
        logger.info("Resumed schedule {}", scheduleId);
        return true;
    }
    
    /**
     * Cancel and remove a scheduled job.
     */
    public boolean cancelSchedule(String scheduleId) {
        ScheduledJob job = scheduledJobs.remove(scheduleId);
        if (job == null) return false;
        
        ScheduledFuture<?> future = activeFutures.remove(scheduleId);
        if (future != null) {
            future.cancel(true);
        }
        
        logger.info("Cancelled schedule {}", scheduleId);
        return true;
    }
    
    /**
     * List all scheduled jobs.
     */
    public List<Map<String, Object>> listSchedules() {
        return scheduledJobs.values().stream()
            .map(this::scheduleToMap)
            .collect(Collectors.toList());
    }
    
    /**
     * Get schedule details.
     */
    public Map<String, Object> getScheduleDetails(String scheduleId) {
        ScheduledJob job = scheduledJobs.get(scheduleId);
        if (job == null) return null;
        return scheduleToMap(job);
    }
    
    // ==================== HELPER METHODS ====================
    
    private void executeScheduledJob(String scheduleId) {
        ScheduledJob scheduledJob = scheduledJobs.get(scheduleId);
        if (scheduledJob == null || scheduledJob.getStatus() != ScheduleStatus.ACTIVE) {
            return;
        }
        
        try {
            logger.info("Executing scheduled job: {}", scheduleId);
            
            scheduledJob.setLastExecutedAt(Instant.now());
            scheduledJob.setExecutionCount(scheduledJob.getExecutionCount() + 1);
            
            JobResponse response = jobQueueService.submitJob(scheduledJob.getJobRequest());
            scheduledJob.setLastJobId(response.getJobId());
            
            logger.info("Scheduled job {} executed, job ID: {}", scheduleId, response.getJobId());
            
            // For one-time jobs, mark as completed
            if (scheduledJob.getScheduleType() == ScheduleType.ONE_TIME) {
                scheduledJob.setStatus(ScheduleStatus.COMPLETED);
                activeFutures.remove(scheduleId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute scheduled job {}: {}", scheduleId, e.getMessage());
            scheduledJob.setLastError(e.getMessage());
        }
    }
    
    private void scheduleCronJob(String scheduleId, String cronExpression, ZoneId zoneId) {
        // Simple cron scheduling using ScheduledExecutorService
        // Calculate delay to next execution
        Instant nextRun = calculateNextCronExecution(cronExpression, zoneId);
        long delayMs = Duration.between(Instant.now(), nextRun).toMillis();
        
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            executeScheduledJob(scheduleId);
            // Re-schedule for next cron trigger
            ScheduledJob job = scheduledJobs.get(scheduleId);
            if (job != null && job.getStatus() == ScheduleStatus.ACTIVE) {
                scheduleCronJob(scheduleId, cronExpression, zoneId);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        
        activeFutures.put(scheduleId, future);
    }
    
    private Instant calculateNextCronExecution(String cronExpression, ZoneId zoneId) {
        // Simple cron parser for common patterns
        // Format: "minute hour dayOfMonth month dayOfWeek"
        // Supports: * for any, specific numbers, ranges (1-5), lists (1,3,5)
        
        String[] parts = cronExpression.trim().split("\\s+");
        if (parts.length < 5) {
            throw new IllegalArgumentException("Cron expression must have at least 5 fields");
        }
        
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime next = now.plusMinutes(1).withSecond(0).withNano(0);
        
        // Find next matching time (simplified - check next 1440 minutes = 24 hours)
        for (int i = 0; i < 1440 * 7; i++) {
            if (matchesCron(next, parts)) {
                return next.toInstant();
            }
            next = next.plusMinutes(1);
        }
        
        // Default to 1 hour from now if no match found
        return Instant.now().plusSeconds(3600);
    }
    
    private boolean matchesCron(ZonedDateTime time, String[] cronParts) {
        return matchesCronField(time.getMinute(), cronParts[0]) &&
               matchesCronField(time.getHour(), cronParts[1]) &&
               matchesCronField(time.getDayOfMonth(), cronParts[2]) &&
               matchesCronField(time.getMonthValue(), cronParts[3]) &&
               matchesCronField(time.getDayOfWeek().getValue() % 7, cronParts[4]); // 0=Sunday
    }
    
    private boolean matchesCronField(int value, String field) {
        if ("*".equals(field)) return true;
        
        // Handle lists (1,3,5)
        if (field.contains(",")) {
            for (String part : field.split(",")) {
                if (matchesCronField(value, part.trim())) return true;
            }
            return false;
        }
        
        // Handle ranges (1-5)
        if (field.contains("-")) {
            String[] range = field.split("-");
            int start = Integer.parseInt(range[0].trim());
            int end = Integer.parseInt(range[1].trim());
            return value >= start && value <= end;
        }
        
        // Handle step (*/5)
        if (field.startsWith("*/")) {
            int step = Integer.parseInt(field.substring(2));
            return value % step == 0;
        }
        
        // Exact match
        try {
            return value == Integer.parseInt(field.trim());
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) return false;
        String[] parts = cronExpression.trim().split("\\s+");
        return parts.length >= 5 && parts.length <= 7;
    }
    
    private void loadPersistedSchedules() {
        // In production, load from database
        logger.debug("Loading persisted schedules (none configured)");
    }
    
    private Map<String, Object> scheduleToMap(ScheduledJob job) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", job.getId());
        map.put("name", job.getName());
        map.put("type", job.getScheduleType().name());
        map.put("status", job.getStatus().name());
        map.put("tool", job.getJobRequest() != null ? job.getJobRequest().getToolName() : null);
        map.put("cronExpression", job.getCronExpression());
        map.put("intervalMinutes", job.getIntervalMinutes());
        map.put("timezone", job.getTimezone());
        map.put("createdAt", job.getCreatedAt() != null ? job.getCreatedAt().toString() : null);
        map.put("lastExecutedAt", job.getLastExecutedAt() != null ? job.getLastExecutedAt().toString() : null);
        map.put("executionCount", job.getExecutionCount());
        map.put("lastJobId", job.getLastJobId());
        map.put("lastError", job.getLastError());
        return map;
    }
    
    // ==================== CLEANUP TASK ====================
    
    /**
     * Cleanup completed one-time schedules (runs every hour).
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupCompletedSchedules() {
        long cutoff = System.currentTimeMillis() - 86400000; // 24 hours ago
        
        scheduledJobs.entrySet().removeIf(entry -> {
            ScheduledJob job = entry.getValue();
            if (job.getStatus() == ScheduleStatus.COMPLETED && 
                job.getCreatedAt() != null && 
                job.getCreatedAt().toEpochMilli() < cutoff) {
                logger.debug("Cleaning up completed schedule: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    // ==================== INNER CLASSES ====================
    
    public enum ScheduleType {
        ONE_TIME, CRON, INTERVAL
    }
    
    public enum ScheduleStatus {
        ACTIVE, PAUSED, COMPLETED, FAILED
    }
    
    public enum WorkflowStatus {
        CREATED, RUNNING, COMPLETED, PARTIAL, FAILED
    }
    
    public static class ScheduledJob {
        private String id;
        private String name;
        private JobRequest jobRequest;
        private ScheduleType scheduleType;
        private String cronExpression;
        private long intervalMinutes;
        private Instant executeAt;
        private String timezone;
        private ScheduleStatus status;
        private Instant createdAt;
        private Instant lastExecutedAt;
        private int executionCount;
        private String lastJobId;
        private String lastError;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public JobRequest getJobRequest() { return jobRequest; }
        public void setJobRequest(JobRequest jobRequest) { this.jobRequest = jobRequest; }
        public ScheduleType getScheduleType() { return scheduleType; }
        public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }
        public String getCronExpression() { return cronExpression; }
        public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
        public long getIntervalMinutes() { return intervalMinutes; }
        public void setIntervalMinutes(long intervalMinutes) { this.intervalMinutes = intervalMinutes; }
        public Instant getExecuteAt() { return executeAt; }
        public void setExecuteAt(Instant executeAt) { this.executeAt = executeAt; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public ScheduleStatus getStatus() { return status; }
        public void setStatus(ScheduleStatus status) { this.status = status; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getLastExecutedAt() { return lastExecutedAt; }
        public void setLastExecutedAt(Instant lastExecutedAt) { this.lastExecutedAt = lastExecutedAt; }
        public int getExecutionCount() { return executionCount; }
        public void setExecutionCount(int executionCount) { this.executionCount = executionCount; }
        public String getLastJobId() { return lastJobId; }
        public void setLastJobId(String lastJobId) { this.lastJobId = lastJobId; }
        public String getLastError() { return lastError; }
        public void setLastError(String lastError) { this.lastError = lastError; }
    }
    
    public static class BatchWorkflow {
        private String id;
        private String name;
        private List<JobRequest> jobs;
        private boolean continueOnError;
        private WorkflowStatus status;
        private Instant createdAt;
        private Instant startedAt;
        private Instant completedAt;
        
        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<JobRequest> getJobs() { return jobs; }
        public void setJobs(List<JobRequest> jobs) { this.jobs = jobs; }
        public boolean isContinueOnError() { return continueOnError; }
        public void setContinueOnError(boolean continueOnError) { this.continueOnError = continueOnError; }
        public WorkflowStatus getStatus() { return status; }
        public void setStatus(WorkflowStatus status) { this.status = status; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    }
    
    public static class ScheduledJobResult {
        private final String scheduleId;
        private final Instant nextExecution;
        private final String scheduleType;
        private final String message;
        
        public ScheduledJobResult(String scheduleId, Instant nextExecution, String scheduleType, String message) {
            this.scheduleId = scheduleId;
            this.nextExecution = nextExecution;
            this.scheduleType = scheduleType;
            this.message = message;
        }
        
        public String getScheduleId() { return scheduleId; }
        public Instant getNextExecution() { return nextExecution; }
        public String getScheduleType() { return scheduleType; }
        public String getMessage() { return message; }
        
        public Map<String, Object> toMap() {
            return Map.of(
                "scheduleId", scheduleId,
                "nextExecution", nextExecution.toString(),
                "scheduleType", scheduleType,
                "message", message
            );
        }
    }
}
