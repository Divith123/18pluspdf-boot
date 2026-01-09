package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for cropping PDF pages
 * API Endpoint: POST /api/pdf/crop-pages
 */
public class CropPagesRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private Double left = 0.0; // percentage from left edge
    
    private Double top = 0.0; // percentage from top edge
    
    private Double right = 0.0; // percentage from right edge
    
    private Double bottom = 0.0; // percentage from bottom edge
    
    private String pageRange = "all"; // "all" or "1-5" or "1,3,5"
    
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public Double getLeft() { return left; }
    public void setLeft(Double left) { this.left = left; }

    public Double getTop() { return top; }
    public void setTop(Double top) { this.top = top; }

    public Double getRight() { return right; }
    public void setRight(Double right) { this.right = right; }

    public Double getBottom() { return bottom; }
    public void setBottom(Double bottom) { this.bottom = bottom; }

    public String getPageRange() { return pageRange; }
    public void setPageRange(String pageRange) { this.pageRange = pageRange; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}