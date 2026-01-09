package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for OCR processing of PDF files
 * API Endpoint: POST /api/pdf/ocr-pdf
 */
public class OCRPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String language = "eng";
    
    private Integer dpi = 300;
    
    private String outputFileName;
    
    private Boolean makeSearchable = true;
    
    private Boolean preserveOriginal = false;
    
    private String engine = "tesseract"; // tesseract, others

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Integer getDpi() { return dpi; }
    public void setDpi(Integer dpi) { this.dpi = dpi; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public Boolean getMakeSearchable() { return makeSearchable; }
    public void setMakeSearchable(Boolean makeSearchable) { this.makeSearchable = makeSearchable; }

    public Boolean getPreserveOriginal() { return preserveOriginal; }
    public void setPreserveOriginal(Boolean preserveOriginal) { this.preserveOriginal = preserveOriginal; }

    public String getEngine() { return engine; }
    public void setEngine(String engine) { this.engine = engine; }
}