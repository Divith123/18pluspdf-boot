package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for converting Office documents to PDF
 * API Endpoint: POST /api/pdf/office-to-pdf
 * Supports: DOC, DOCX, XLS, XLSX, PPT, PPTX, ODT, ODS, ODP
 */
public class OfficeToPDFRequest {
    
    @NotNull(message = "Office file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private Boolean hideComments = false;
    
    private Boolean exportBookmarks = true;
    
    private Integer timeoutSeconds = 60;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public Boolean getHideComments() { return hideComments; }
    public void setHideComments(Boolean hideComments) { this.hideComments = hideComments; }

    public Boolean getExportBookmarks() { return exportBookmarks; }
    public void setExportBookmarks(Boolean exportBookmarks) { this.exportBookmarks = exportBookmarks; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}