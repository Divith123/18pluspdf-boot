package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for comparing two PDF files
 * API Endpoint: POST /api/pdf/compare-pdfs
 */
public class ComparePDFsRequest {
    
    @NotNull(message = "First PDF file is required")
    private MultipartFile file1;
    
    @NotNull(message = "Second PDF file is required")
    private MultipartFile file2;
    
    private String outputFileName;
    
    private Boolean compareText = true;
    
    private Boolean compareImages = true;
    
    private Boolean compareLayout = true;
    
    private Double tolerance = 0.1;

    // Getters and Setters
    public MultipartFile getFile1() { return file1; }
    public void setFile1(MultipartFile file1) { this.file1 = file1; }

    public MultipartFile getFile2() { return file2; }
    public void setFile2(MultipartFile file2) { this.file2 = file2; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public Boolean getCompareText() { return compareText; }
    public void setCompareText(Boolean compareText) { this.compareText = compareText; }

    public Boolean getCompareImages() { return compareImages; }
    public void setCompareImages(Boolean compareImages) { this.compareImages = compareImages; }

    public Boolean getCompareLayout() { return compareLayout; }
    public void setCompareLayout(Boolean compareLayout) { this.compareLayout = compareLayout; }

    public Double getTolerance() { return tolerance; }
    public void setTolerance(Double tolerance) { this.tolerance = tolerance; }
}