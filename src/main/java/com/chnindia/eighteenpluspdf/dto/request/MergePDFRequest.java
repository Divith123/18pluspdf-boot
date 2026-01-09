package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO for merging multiple PDF files into one
 * API Endpoint: POST /api/pdf/merge
 */
public class MergePDFRequest {
    
    @NotEmpty(message = "At least one PDF file is required")
    private List<MultipartFile> files;
    
    private String outputFileName;
    
    private Boolean preserveBookmarks = true;
    
    private Boolean removeAnnotations = false;

    // Getters and Setters
    public List<MultipartFile> getFiles() { return files; }
    public void setFiles(List<MultipartFile> files) { this.files = files; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public Boolean getPreserveBookmarks() { return preserveBookmarks; }
    public void setPreserveBookmarks(Boolean preserveBookmarks) { this.preserveBookmarks = preserveBookmarks; }

    public Boolean getRemoveAnnotations() { return removeAnnotations; }
    public void setRemoveAnnotations(Boolean removeAnnotations) { this.removeAnnotations = removeAnnotations; }
}