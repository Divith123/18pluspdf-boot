package com.chnindia.eighteenpluspdf.repository;

import com.chnindia.eighteenpluspdf.model.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<JobStatus, String> {
    
    // Find jobs by status
    List<JobStatus> findByStatus(JobStatus.Status status);
    
    // Find recent jobs (last 7 days)
    @Query("SELECT j FROM JobStatus j WHERE j.createdAt >= :startDate ORDER BY j.createdAt DESC")
    List<JobStatus> findRecentJobs(@Param("startDate") LocalDateTime startDate);
    
    // Find stale jobs (incomplete and older than 1 day)
    @Query("SELECT j FROM JobStatus j WHERE j.completedAt IS NULL AND j.createdAt < :cutoffDate")
    List<JobStatus> findStaleJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Find jobs by tool name
    List<JobStatus> findByToolName(String toolName);
    
    // Find jobs by status and tool name
    List<JobStatus> findByStatusAndToolName(JobStatus.Status status, String toolName);
    
    // Find jobs created between dates
    List<JobStatus> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    // Find failed jobs in last 24 hours
    @Query("SELECT j FROM JobStatus j WHERE j.status = 'FAILED' AND j.createdAt >= :startDate")
    List<JobStatus> findFailedJobsSince(@Param("startDate") LocalDateTime startDate);
    
    // Get job statistics
    @Query("SELECT COUNT(j), j.status FROM JobStatus j GROUP BY j.status")
    List<Object[]> getJobStatistics();
    
    // Get average processing time by tool
    @Query("SELECT j.toolName, AVG(j.processingTimeMs) FROM JobStatus j WHERE j.status = 'COMPLETED' GROUP BY j.toolName")
    List<Object[]> getAverageProcessingTimeByTool();
    
    // Clean up old completed jobs
    @Modifying
    @Query("DELETE FROM JobStatus j WHERE j.status = 'COMPLETED' AND j.completedAt < :cutoffDate")
    int cleanupOldJobs(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Update progress
    @Modifying
    @Query("UPDATE JobStatus j SET j.progress = :progress, j.currentOperation = :operation, j.updatedAt = CURRENT_TIMESTAMP WHERE j.id = :jobId")
    void updateProgress(@Param("jobId") String jobId, @Param("progress") Integer progress, @Param("operation") String operation);
    
    // Find by result hash (for deduplication)
    Optional<JobStatus> findByResultHash(String resultHash);
    
    // Find active jobs (not completed or failed)
    @Query("SELECT j FROM JobStatus j WHERE j.status IN ('PENDING', 'PROCESSING')")
    List<JobStatus> findActiveJobs();
}