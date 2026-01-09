package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for converting Markdown to PDF
 * API Endpoint: POST /api/pdf/markdown-to-pdf
 */
public class MarkdownToPDFRequest {
    
    @NotNull(message = "Markdown file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private String pageSize = "A4";
    
    private Integer margin = 36;
    
    private String theme = "default"; // default, dark, light
    
    private Boolean enableCodeHighlighting = true;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getPageSize() { return pageSize; }
    public void setPageSize(String pageSize) { this.pageSize = pageSize; }

    public Integer getMargin() { return margin; }
    public void setMargin(Integer margin) { this.margin = margin; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public Boolean getEnableCodeHighlighting() { return enableCodeHighlighting; }
    public void setEnableCodeHighlighting(Boolean enableCodeHighlighting) { this.enableCodeHighlighting = enableCodeHighlighting; }
}