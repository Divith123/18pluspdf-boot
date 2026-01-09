package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for converting HTML to PDF
 * API Endpoint: POST /api/pdf/html-to-pdf
 */
public class HTMLToPDFRequest {
    
    @NotNull(message = "HTML file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private String pageSize = "A4";
    
    private Integer margin = 20;
    
    private Boolean enableImages = true;
    
    private Boolean enableJavaScript = false;
    
    private Integer timeoutSeconds = 30;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public Integer getMargin() { return margin; }
    public void setMargin(Integer margin) { this.margin = margin; }

    public Boolean getEnableImages() { return enableImages; }
    public void setEnableImages(Boolean enableImages) { this.enableImages = enableImages; }

    public Boolean getEnableJavaScript() { return enableJavaScript; }
    public void setEnableJavaScript(Boolean enableJavaScript) { this.enableJavaScript = enableJavaScript; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}