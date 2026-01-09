package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for extracting text from PDF
 * API Endpoint: POST /api/pdf/pdf-to-text
 */
public class PDFToTextRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String pageRange = "all";
    
    private String encoding = "UTF-8";
    
    private Boolean preserveFormatting = false;
    
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getPageRange() { return pageRange; }
    public void setPageRange(String pageRange) { this.pageRange = pageRange; }

    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }

    public Boolean getPreserveFormatting() { return preserveFormatting; }
    public void setPreserveFormatting(Boolean preserveFormatting) { this.preserveFormatting = preserveFormatting; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}