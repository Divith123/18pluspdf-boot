package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for removing pages from PDF files
 * API Endpoint: POST /api/pdf/remove-pages
 */
public class RemovePagesRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @NotBlank(message = "Page ranges to remove are required")
    private String pagesToRemove; // e.g., "1-3,5,7-9"
    
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getPagesToRemove() { return pagesToRemove; }
    public void setPagesToRemove(String pagesToRemove) { this.pagesToRemove = pagesToRemove; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}