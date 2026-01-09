package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for converting text to PDF
 * API Endpoint: POST /api/pdf/text-to-pdf
 */
public class TextToPDFRequest {
    
    @NotNull(message = "Text file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private String fontName = "Helvetica";
    
    private Integer fontSize = 12;
    
    private String pageSize = "A4";
    
    private Integer margin = 36;
    
    private String encoding = "UTF-8";

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getFontName() { return fontName; }
    public void setFontName(String fontName) { this.fontName = fontName; }

    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public Integer getMargin() { return margin; }
    public void setMargin(Integer margin) { this.margin = margin; }

    public String getEncoding() { return encoding; }
    public void setEncoding(String encoding) { this.encoding = encoding; }
}