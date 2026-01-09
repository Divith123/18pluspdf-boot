package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO for converting images to PDF
 * API Endpoint: POST /api/pdf/image-to-pdf
 */
public class ImageToPDFRequest {
    
    @NotEmpty(message = "At least one image file is required")
    private List<MultipartFile> images;
    
    private String outputFileName;
    
    private String pageSize = "A4";
    
    private Boolean fitToPage = true;
    
    private Integer margin = 10;

    // Getters and Setters
    public List<MultipartFile> getImages() { return images; }
    public void setImages(List<MultipartFile> images) { this.images = images; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public Boolean getFitToPage() { return fitToPage; }
    public void setFitToPage(Boolean fitToPage) { this.fitToPage = fitToPage; }

    public Integer getMargin() { return margin; }
    public void setMargin(Integer margin) { this.margin = margin; }
}