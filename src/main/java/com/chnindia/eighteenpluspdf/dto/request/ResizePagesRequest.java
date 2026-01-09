package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for resizing PDF pages
 * API Endpoint: POST /api/pdf/resize-pages
 */
public class ResizePagesRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private Integer width; // in points
    
    private Integer height; // in points
    
    private String pageSize = "A4"; // A4, Letter, Legal, etc.
    
    private String pageRange = "all";
    
    private String outputFileName;
    
    private Boolean maintainAspectRatio = true;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public String getPageRange() { return pageRange; }
    public void setPageRange(String pageRange) { this.pageRange = pageRange; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public Boolean getMaintainAspectRatio() { return maintainAspectRatio; }
    public void setMaintainAspectRatio(Boolean maintainAspectRatio) { this.maintainAspectRatio = maintainAspectRatio; }
}