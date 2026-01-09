package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for exporting form data from PDFs.
 * Supports FDF, XFDF, XML, JSON, and CSV export formats.
 */
public class FormExportRequest {
    
    @NotNull(message = "Export format is required")
    private ExportFormat format = ExportFormat.JSON;
    
    private String outputFileName;
    
    private boolean includeEmptyFields = false;
    
    private boolean includeFieldMetadata = false;
    
    private boolean flattenStructure = false;
    
    private String dateFormat = "yyyy-MM-dd";
    
    private String encoding = "UTF-8";
    
    public enum ExportFormat {
        FDF,    // Forms Data Format (Adobe standard)
        XFDF,   // XML Forms Data Format
        XML,    // Generic XML
        JSON,   // JSON format
        CSV     // CSV for tabular data
    }
    
    // Getters and Setters
    
    public ExportFormat getFormat() {
        return format;
    }
    
    public void setFormat(ExportFormat format) {
        this.format = format;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public boolean isIncludeEmptyFields() {
        return includeEmptyFields;
    }
    
    public void setIncludeEmptyFields(boolean includeEmptyFields) {
        this.includeEmptyFields = includeEmptyFields;
    }
    
    public boolean isIncludeFieldMetadata() {
        return includeFieldMetadata;
    }
    
    public void setIncludeFieldMetadata(boolean includeFieldMetadata) {
        this.includeFieldMetadata = includeFieldMetadata;
    }
    
    public boolean isFlattenStructure() {
        return flattenStructure;
    }
    
    public void setFlattenStructure(boolean flattenStructure) {
        this.flattenStructure = flattenStructure;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}
