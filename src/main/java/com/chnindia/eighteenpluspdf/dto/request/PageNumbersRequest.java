package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for adding page numbers to PDF files
 * API Endpoint: POST /api/pdf/add-page-numbers
 */
public class PageNumbersRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String format = "Page {page} of {total}";
    
    private String fontName = "Helvetica";
    
    private Integer fontSize = 12;
    
    private String color = "#000000";
    
    private String position = "bottom-center"; // top-center, bottom-center, top-right, bottom-right, etc.
    
    private Integer margin = 36; // points
    
    private String startPage; // "1" or "1-5" or "all"

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getFontName() { return fontName; }
    public void setFontName(String fontName) { this.fontName = fontName; }

    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getMargin() { return margin; }
    public void setMargin(Integer margin) { this.margin = margin; }

    public String getStartPage() { return startPage; }
    public void setStartPage(String startPage) { this.startPage = startPage; }
}