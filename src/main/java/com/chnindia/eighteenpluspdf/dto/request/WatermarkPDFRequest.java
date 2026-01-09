package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for adding watermarks to PDF files
 * API Endpoint: POST /api/pdf/watermark
 */
public class WatermarkPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @NotBlank(message = "Watermark text is required")
    private String watermarkText;
    
    private String fontName = "Helvetica";
    
    private Integer fontSize = 48;
    
    private String color = "#808080"; // Gray with transparency
    
    private Double opacity = 0.3;
    
    private String position = "center"; // center, top-left, top-right, bottom-left, bottom-right
    
    private Integer rotation = 45;
    
    private Boolean diagonal = true;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getWatermarkText() { return watermarkText; }
    public void setWatermarkText(String watermarkText) { this.watermarkText = watermarkText; }

    public String getFontName() { return fontName; }
    public void setFontName(String fontName) { this.fontName = fontName; }

    public Integer getFontSize() { return fontSize; }
    public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public Double getOpacity() { return opacity; }
    public void setOpacity(Double opacity) { this.opacity = opacity; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Integer getRotation() { return rotation; }
    public void setRotation(Integer rotation) { this.rotation = rotation; }

    public Boolean getDiagonal() { return diagonal; }
    public void setDiagonal(Boolean diagonal) { this.diagonal = diagonal; }
}