package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for splitting PDF files
 * API Endpoint: POST /api/pdf/split
 */
public class SplitPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @Min(1)
    private Integer splitEveryNPages = 1;
    
    private String outputPrefix;
    
    private Boolean splitByPageRanges = false;
    
    private String pageRanges; // e.g., "1-3,5,7-9"

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public Integer getSplitEveryNPages() { return splitEveryNPages; }
    public void setSplitEveryNPages(Integer splitEveryNPages) { this.splitEveryNPages = splitEveryNPages; }

    public String getOutputPrefix() { return outputPrefix; }
    public void setOutputPrefix(String outputPrefix) { this.outputPrefix = outputPrefix; }

    public Boolean getSplitByPageRanges() { return splitByPageRanges; }
    public void setSplitByPageRanges(Boolean splitByPageRanges) { this.splitByPageRanges = splitByPageRanges; }

    public String getPageRanges() { return pageRanges; }
    public void setPageRanges(String pageRanges) { this.pageRanges = pageRanges; }
}