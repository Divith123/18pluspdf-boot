package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for optimizing PDF files
 * API Endpoint: POST /api/pdf/optimize
 */
public class OptimizeRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private Boolean removeUnusedObjects = true;
    
    private Boolean compressImages = true;
    
    private Integer imageQuality = 85;
    
    private Boolean subsetFonts = true;
    
    private Boolean linearize = false;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public Boolean getRemoveUnusedObjects() { return removeUnusedObjects; }
    public void setRemoveUnusedObjects(Boolean removeUnusedObjects) { this.removeUnusedObjects = removeUnusedObjects; }

    public Boolean getCompressImages() { return compressImages; }
    public void setCompressImages(Boolean compressImages) { this.compressImages = compressImages; }

    public Integer getImageQuality() { return imageQuality; }
    public void setImageQuality(Integer imageQuality) { this.imageQuality = imageQuality; }

    public Boolean getSubsetFonts() { return subsetFonts; }
    public void setSubsetFonts(Boolean subsetFonts) { this.subsetFonts = subsetFonts; }

    public Boolean getLinearize() { return linearize; }
    public void setLinearize(Boolean linearize) { this.linearize = linearize; }
}