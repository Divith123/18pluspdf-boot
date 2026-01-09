package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for converting PDF to PDF/A
 * API Endpoint: POST /api/pdf/pdfa-convert
 */
public class PDFAConvertRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private String complianceLevel = "2b"; // 1b, 2b, 2u, 3b
    
    private Boolean embedFonts = true;
    
    private Boolean validateOutput = true;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getComplianceLevel() { return complianceLevel; }
    public void setComplianceLevel(String complianceLevel) { this.complianceLevel = complianceLevel; }

    public Boolean getEmbedFonts() { return embedFonts; }
    public void setEmbedFonts(Boolean embedFonts) { this.embedFonts = embedFonts; }

    public Boolean getValidateOutput() { return validateOutput; }
    public void setValidateOutput(Boolean validateOutput) { this.validateOutput = validateOutput; }
}