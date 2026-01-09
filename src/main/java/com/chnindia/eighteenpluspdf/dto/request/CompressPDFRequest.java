package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for compressing PDF files
 * API Endpoint: POST /api/pdf/compress
 */
public class CompressPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private Double compressionQuality = 0.85;
    
    private Integer imageQuality = 85;
    
    private Boolean removeMetadata = false;
    
    private Boolean optimizeImages = true;
    
    private Integer maxImageDpi = 150;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public Double getCompressionQuality() { return compressionQuality; }
    public void setCompressionQuality(Double compressionQuality) { this.compressionQuality = compressionQuality; }

    public Integer getImageQuality() { return imageQuality; }
    public void setImageQuality(Integer imageQuality) { this.imageQuality = imageQuality; }

    public Boolean getRemoveMetadata() { return removeMetadata; }
    public void setRemoveMetadata(Boolean removeMetadata) { this.removeMetadata = removeMetadata; }

    public Boolean getOptimizeImages() { return optimizeImages; }
    public void setOptimizeImages(Boolean optimizeImages) { this.optimizeImages = optimizeImages; }

    public Integer getMaxImageDpi() { return maxImageDpi; }
    public void setMaxImageDpi(Integer maxImageDpi) { this.maxImageDpi = maxImageDpi; }
}