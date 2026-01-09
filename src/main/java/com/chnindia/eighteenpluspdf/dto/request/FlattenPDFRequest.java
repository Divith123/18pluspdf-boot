package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for flattening a PDF document.
 * Flattening converts interactive elements to static content.
 */
public class FlattenPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private Boolean flattenForms = true;
    private Boolean flattenAnnotations = true;
    private Boolean flattenComments = true;
    private Boolean flattenLayers = false;
    private Boolean flattenTransparency = false;
    private String pageRange = "all";
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    
    public Boolean getFlattenForms() { return flattenForms; }
    public void setFlattenForms(Boolean flattenForms) { this.flattenForms = flattenForms; }
    
    public Boolean getFlattenAnnotations() { return flattenAnnotations; }
    public void setFlattenAnnotations(Boolean flattenAnnotations) { this.flattenAnnotations = flattenAnnotations; }
    
    public Boolean getFlattenComments() { return flattenComments; }
    public void setFlattenComments(Boolean flattenComments) { this.flattenComments = flattenComments; }
    
    public Boolean getFlattenLayers() { return flattenLayers; }
    public void setFlattenLayers(Boolean flattenLayers) { this.flattenLayers = flattenLayers; }
    
    public Boolean getFlattenTransparency() { return flattenTransparency; }
    public void setFlattenTransparency(Boolean flattenTransparency) { this.flattenTransparency = flattenTransparency; }
    
    public String getPageRange() { return pageRange; }
    public void setPageRange(String pageRange) { this.pageRange = pageRange; }
    
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}
