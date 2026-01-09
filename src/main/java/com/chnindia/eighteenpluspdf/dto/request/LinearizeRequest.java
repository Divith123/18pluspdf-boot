package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for linearizing PDF (web optimization)
 * API Endpoint: POST /api/pdf/linearize
 */
public class LinearizeRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private Boolean optimizeForWeb = true;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public Boolean getOptimizeForWeb() { return optimizeForWeb; }
    public void setOptimizeForWeb(Boolean optimizeForWeb) { this.optimizeForWeb = optimizeForWeb; }
}