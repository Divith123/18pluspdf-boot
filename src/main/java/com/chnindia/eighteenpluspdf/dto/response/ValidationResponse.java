package com.chnindia.eighteenpluspdf.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for PDF quality validation.
 */
public class ValidationResponse {
    
    private String jobId;
    private boolean valid;
    private ValidationLevel validationLevel;
    private List<ValidationIssue> issues;
    private ValidationStatistics statistics;
    private PDFMetrics metrics;
    private LocalDateTime validatedAt;
    
    public enum ValidationLevel {
        PASSED,          // All checks passed
        WARNINGS,        // Passed with warnings
        FAILED,          // Critical issues found
        ERROR            // Could not complete validation
    }
    
    public static class ValidationIssue {
        private IssueSeverity severity;
        private String category;
        private String code;
        private String message;
        private String details;
        private Integer pageNumber;
        private String location;
        private String recommendation;
        
        public enum IssueSeverity {
            INFO,
            WARNING,
            ERROR,
            CRITICAL
        }
        
        // Getters and Setters
        public IssueSeverity getSeverity() { return severity; }
        public void setSeverity(IssueSeverity severity) { this.severity = severity; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getRecommendation() { return recommendation; }
        public void setRecommendation(String recommendation) { this.recommendation = recommendation; }
    }
    
    public static class ValidationStatistics {
        private int totalChecks;
        private int passedChecks;
        private int warningChecks;
        private int failedChecks;
        private int infoCount;
        private int warningCount;
        private int errorCount;
        private int criticalCount;
        
        // Getters and Setters
        public int getTotalChecks() { return totalChecks; }
        public void setTotalChecks(int totalChecks) { this.totalChecks = totalChecks; }
        
        public int getPassedChecks() { return passedChecks; }
        public void setPassedChecks(int passedChecks) { this.passedChecks = passedChecks; }
        
        public int getWarningChecks() { return warningChecks; }
        public void setWarningChecks(int warningChecks) { this.warningChecks = warningChecks; }
        
        public int getFailedChecks() { return failedChecks; }
        public void setFailedChecks(int failedChecks) { this.failedChecks = failedChecks; }
        
        public int getInfoCount() { return infoCount; }
        public void setInfoCount(int infoCount) { this.infoCount = infoCount; }
        
        public int getWarningCount() { return warningCount; }
        public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
        
        public int getErrorCount() { return errorCount; }
        public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
        
        public int getCriticalCount() { return criticalCount; }
        public void setCriticalCount(int criticalCount) { this.criticalCount = criticalCount; }
    }
    
    public static class PDFMetrics {
        private String pdfVersion;
        private int pageCount;
        private long fileSizeBytes;
        private boolean isEncrypted;
        private boolean hasAcroForm;
        private boolean isTagged;
        private boolean isSearchable;
        private boolean isPDFA;
        private String pdfaConformance;
        private boolean isLinearized;
        private int fontCount;
        private int imageCount;
        private int annotationCount;
        private int bookmarkCount;
        private List<String> embeddedFonts;
        private Map<String, Object> additionalMetrics;
        
        // Getters and Setters
        public String getPdfVersion() { return pdfVersion; }
        public void setPdfVersion(String pdfVersion) { this.pdfVersion = pdfVersion; }
        
        public int getPageCount() { return pageCount; }
        public void setPageCount(int pageCount) { this.pageCount = pageCount; }
        
        public long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
        
        public boolean isEncrypted() { return isEncrypted; }
        public void setEncrypted(boolean encrypted) { isEncrypted = encrypted; }
        
        public boolean isHasAcroForm() { return hasAcroForm; }
        public void setHasAcroForm(boolean hasAcroForm) { this.hasAcroForm = hasAcroForm; }
        
        public boolean isTagged() { return isTagged; }
        public void setTagged(boolean tagged) { isTagged = tagged; }
        
        public boolean isSearchable() { return isSearchable; }
        public void setSearchable(boolean searchable) { isSearchable = searchable; }
        
        public boolean isPDFA() { return isPDFA; }
        public void setPDFA(boolean PDFA) { isPDFA = PDFA; }
        
        public String getPdfaConformance() { return pdfaConformance; }
        public void setPdfaConformance(String pdfaConformance) { this.pdfaConformance = pdfaConformance; }
        
        public boolean isLinearized() { return isLinearized; }
        public void setLinearized(boolean linearized) { isLinearized = linearized; }
        
        public int getFontCount() { return fontCount; }
        public void setFontCount(int fontCount) { this.fontCount = fontCount; }
        
        public int getImageCount() { return imageCount; }
        public void setImageCount(int imageCount) { this.imageCount = imageCount; }
        
        public int getAnnotationCount() { return annotationCount; }
        public void setAnnotationCount(int annotationCount) { this.annotationCount = annotationCount; }
        
        public int getBookmarkCount() { return bookmarkCount; }
        public void setBookmarkCount(int bookmarkCount) { this.bookmarkCount = bookmarkCount; }
        
        public List<String> getEmbeddedFonts() { return embeddedFonts; }
        public void setEmbeddedFonts(List<String> embeddedFonts) { this.embeddedFonts = embeddedFonts; }
        
        public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
        public void setAdditionalMetrics(Map<String, Object> additionalMetrics) { this.additionalMetrics = additionalMetrics; }
    }
    
    // Main class Getters and Setters
    
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public ValidationLevel getValidationLevel() { return validationLevel; }
    public void setValidationLevel(ValidationLevel validationLevel) { this.validationLevel = validationLevel; }
    
    public List<ValidationIssue> getIssues() { return issues; }
    public void setIssues(List<ValidationIssue> issues) { this.issues = issues; }
    
    public ValidationStatistics getStatistics() { return statistics; }
    public void setStatistics(ValidationStatistics statistics) { this.statistics = statistics; }
    
    public PDFMetrics getMetrics() { return metrics; }
    public void setMetrics(PDFMetrics metrics) { this.metrics = metrics; }
    
    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }
}
