package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.JobRequest;
import com.chnindia.eighteenpluspdf.dto.JobResponse;
import com.chnindia.eighteenpluspdf.dto.response.JobStatusResponse;
import com.chnindia.eighteenpluspdf.exception.JobNotFoundException;
import com.chnindia.eighteenpluspdf.model.JobStatus;
import com.chnindia.eighteenpluspdf.repository.JobRepository;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import com.chnindia.eighteenpluspdf.util.SecurityUtil;
import com.chnindia.eighteenpluspdf.worker.PDFWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JobQueueService {
    private static final Logger logger = LoggerFactory.getLogger(JobQueueService.class);
    
    @Autowired
    private JobRepository jobRepository;
    
    @Autowired
    private PDFWorker pdfWorker;
    
    @Autowired
    private FileUtil fileUtil;
    
    @Autowired
    private SecurityUtil securityUtil;
    
    @Value("${app.job-queue.max-retries:3}")
    private int maxRetries;
    
    @Value("${app.job-queue.timeout-minutes:30}")
    private int timeoutMinutes;
    
    @Value("${app.job-queue.retry-delay-ms:5000}")
    private long retryDelayMs;
    
    /**
     * Submit a new job to the queue
     */
    public JobResponse submitJob(JobRequest request) {
        String jobId = UUID.randomUUID().toString();
        String sanitizedFileName = securityUtil.sanitizeInput(request.getFile().getOriginalFilename());
        
        // Create job status
        JobStatus jobStatus = new JobStatus(
            jobId,
            request.getToolName(),
            sanitizedFileName
        );
        
        // Calculate file hash for deduplication
        try {
            Path tempFile = fileUtil.saveTempFile(request.getFile());
            String fileHash = fileUtil.calculateFileHash(tempFile);
            jobStatus.setResultHash(fileHash);
            jobStatus.setFileSize(Files.size(tempFile));
            
            // Check for duplicate jobs
            jobRepository.findByResultHash(fileHash).ifPresent(existing -> {
                if (existing.getStatus() == JobStatus.Status.COMPLETED) {
                    logger.info("Duplicate job detected, returning existing result: {}", existing.getId());
                    throw new IllegalArgumentException("Duplicate job detected with ID: " + existing.getId());
                }
            });
            
            // Clean up temp file immediately (will be recreated in worker)
            fileUtil.cleanupTempFile(tempFile);
        } catch (IOException e) {
            logger.warn("Could not calculate file hash for deduplication: {}", e.getMessage());
        }
        
        jobStatus = jobRepository.save(jobStatus);
        
        // Process asynchronously
        processJobAsync(jobId, request);
        
        return new JobResponse(jobStatus);
    }
    
    /**
     * Process job asynchronously with retry logic
     */
    @Async("taskExecutor")
    public void processJobAsync(String jobId, JobRequest request) {
        JobStatus jobStatus = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));
        
        Path tempFile = null;
        long startTime = System.currentTimeMillis();
        int attempt = 0;
        
        while (attempt < maxRetries) {
            try {
                attempt++;
                
                // Update status to processing
                jobStatus.setStatus(JobStatus.Status.PROCESSING);
                jobStatus.setCurrentOperation("Starting processing");
                jobStatus.setProgress(5);
                jobRepository.save(jobStatus);
                
                // Save uploaded file to temp location
                tempFile = fileUtil.saveTempFile(request.getFile());
                
                // Update progress
                jobStatus.setProgress(10);
                jobStatus.setCurrentOperation("File uploaded");
                jobRepository.save(jobStatus);
                
                // Process based on tool name
                Map<String, Object> result = pdfWorker.process(
                    request.getToolName(),
                    tempFile,
                    request.getParameters(),
                    jobStatus
                );
                
                // Update job status
                jobStatus.setStatus(JobStatus.Status.COMPLETED);
                jobStatus.setProgress(100);
                jobStatus.setCurrentOperation("Completed");
                jobStatus.setResultUrl((String) result.get("resultUrl"));
                jobStatus.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                jobStatus.setCompletedAt(LocalDateTime.now());
                
                jobRepository.save(jobStatus);
                
                logger.info("Job {} completed successfully for tool {} in {}ms (attempt {}/{})", 
                    jobId, request.getToolName(), 
                    System.currentTimeMillis() - startTime, attempt, maxRetries);
                
                break; // Success, exit retry loop
                
            } catch (Exception e) {
                logger.error("Job {} failed on attempt {}: {}", jobId, attempt, e.getMessage(), e);
                
                if (attempt >= maxRetries) {
                    // Final failure
                    jobStatus.setStatus(JobStatus.Status.FAILED);
                    jobStatus.setErrorMessage(e.getMessage());
                    jobStatus.setProgress(0);
                    jobStatus.setCurrentOperation("Failed");
                    jobStatus.setProcessingTimeMs(System.currentTimeMillis() - startTime);
                    jobStatus.setCompletedAt(LocalDateTime.now());
                    jobRepository.save(jobStatus);
                    break;
                } else {
                    // Retry with delay
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                // Clean up temp file
                if (tempFile != null) {
                    fileUtil.cleanupTempFile(tempFile);
                }
            }
        }
    }
    
    /**
     * Get job status by ID
     */
    public JobStatusResponse getJobStatus(String jobId) {
        JobStatus jobStatus = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));
        
        return new JobStatusResponse(jobStatus);
    }
    
    /**
     * Cancel a running job
     */
    public boolean cancelJob(String jobId) {
        JobStatus jobStatus = jobRepository.findById(jobId)
            .orElseThrow(() -> new JobNotFoundException(jobId));
        
        if (jobStatus.getStatus() == JobStatus.Status.PENDING || 
            jobStatus.getStatus() == JobStatus.Status.PROCESSING) {
            jobStatus.setStatus(JobStatus.Status.CANCELLED);
            jobStatus.setCompletedAt(LocalDateTime.now());
            jobRepository.save(jobStatus);
            logger.info("Job {} cancelled", jobId);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get all jobs with optional status filter
     */
    public List<JobStatus> getJobs(JobStatus.Status status) {
        if (status != null) {
            return jobRepository.findByStatus(status);
        }
        return jobRepository.findAll();
    }
    
    /**
     * Get recent jobs
     */
    public List<JobStatus> getRecentJobs(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return jobRepository.findRecentJobs(cutoff);
    }
    
    /**
     * Get job statistics
     */
    public Map<String, Object> getStatistics() {
        List<Object[]> stats = jobRepository.getJobStatistics();
        Map<String, Object> result = new java.util.HashMap<>();
        
        for (Object[] stat : stats) {
            Long count = (Long) stat[0];
            JobStatus.Status status = (JobStatus.Status) stat[1];
            result.put(status.name(), count);
        }
        
        return result;
    }
    
    /**
     * Clean up old completed jobs
     */
    @Transactional
    public void cleanupOldJobs(int days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = jobRepository.cleanupOldJobs(cutoff);
        logger.info("Cleaned up {} old jobs", deleted);
    }
    
    /**
     * Get job by result hash
     */
    public JobStatus getJobByHash(String hash) {
        return jobRepository.findByResultHash(hash).orElse(null);
    }
    
    /**
     * Update job progress
     */
    @Transactional
    public void updateProgress(String jobId, int progress, String operation) {
        jobRepository.updateProgress(jobId, progress, operation);
    }
    
    /**
     * Wait for job completion with timeout
     */
    public boolean waitForCompletion(String jobId, long timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            JobStatus jobStatus = jobRepository.findById(jobId).orElse(null);
            if (jobStatus == null) return false;
            
            JobStatus.Status status = jobStatus.getStatus();
            if (status == JobStatus.Status.COMPLETED || status == JobStatus.Status.FAILED) {
                return status == JobStatus.Status.COMPLETED;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Get active jobs count
     */
    public long getActiveJobsCount() {
        return jobRepository.findActiveJobs().size();
    }
}