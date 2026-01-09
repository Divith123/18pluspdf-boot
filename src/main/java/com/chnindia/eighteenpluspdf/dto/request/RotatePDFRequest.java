package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for rotating PDF pages
 * API Endpoint: POST /api/pdf/rotate
 */
public class RotatePDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @NotNull(message = "Rotation angle is required")
    private Integer angle; // 90, 180, 270
    
    private String pageRange; // "all" or "1-5" or "1,3,5"
    
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public Integer getAngle() { return angle; }
    public void setAngle(Integer angle) { this.angle = angle; }

    public String getPageRange() { return pageRange; }
    public void setPageRange(String pageRange) { this.pageRange = pageRange; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}