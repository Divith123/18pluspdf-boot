package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for converting PDF to images
 * API Endpoint: POST /api/pdf/pdf-to-image
 */
public class PDFToImageRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String imageFormat = "png"; // png, jpg, tiff, bmp
    
    private Integer dpi = 300;
    
    private String pageRange = "all";
    
    private String outputPrefix;
    
    private Boolean color = true;
    
    private Double scale = 1.0;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getImageFormat() { return imageFormat; }
    public void setImageFormat(String imageFormat) { this.imageFormat = imageFormat; }

    public Integer getDpi() { return dpi; }
    public void setDpi(Integer dpi) { this.dpi = dpi; }

    public String getPageRange() { return pageRange; }
    public void setPageRange(String pageRange) { this.pageRange = pageRange; }

    public String getOutputPrefix() { return outputPrefix; }
    public void setOutputPrefix(String outputPrefix) { this.outputPrefix = outputPrefix; }

    public Boolean getColor() { return color; }
    public void setColor(Boolean color) { this.color = color; }

    public Double getScale() { return scale; }
    public void setScale(Double scale) { this.scale = scale; }
}