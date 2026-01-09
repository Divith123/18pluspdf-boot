package com.chnindia.eighteenpluspdf.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for form data extraction and analysis.
 */
public class FormDataResponse {
    
    private String jobId;
    private int fieldCount;
    private List<FormField> fields;
    private Map<String, Object> formData;
    private String format;
    private String exportUrl;
    private FormStatistics statistics;
    private LocalDateTime extractedAt;
    private String error;
    
    public static class FormField {
        private String name;
        private String type;
        private String value;
        private boolean readOnly;
        private boolean required;
        private boolean hasValue;
        private List<String> options;
        private Map<String, Object> metadata;
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        
        public boolean isReadOnly() { return readOnly; }
        public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public boolean isHasValue() { return hasValue; }
        public void setHasValue(boolean hasValue) { this.hasValue = hasValue; }
        
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }
    
    public static class FormStatistics {
        private int totalFields;
        private int filledFields;
        private int emptyFields;
        private int textFields;
        private int checkboxFields;
        private int radioFields;
        private int dropdownFields;
        private int signatureFields;
        private double completionPercentage;
        
        // Getters and Setters
        public int getTotalFields() { return totalFields; }
        public void setTotalFields(int totalFields) { this.totalFields = totalFields; }
        
        public int getFilledFields() { return filledFields; }
        public void setFilledFields(int filledFields) { this.filledFields = filledFields; }
        
        public int getEmptyFields() { return emptyFields; }
        public void setEmptyFields(int emptyFields) { this.emptyFields = emptyFields; }
        
        public int getTextFields() { return textFields; }
        public void setTextFields(int textFields) { this.textFields = textFields; }
        
        public int getCheckboxFields() { return checkboxFields; }
        public void setCheckboxFields(int checkboxFields) { this.checkboxFields = checkboxFields; }
        
        public int getRadioFields() { return radioFields; }
        public void setRadioFields(int radioFields) { this.radioFields = radioFields; }
        
        public int getDropdownFields() { return dropdownFields; }
        public void setDropdownFields(int dropdownFields) { this.dropdownFields = dropdownFields; }
        
        public int getSignatureFields() { return signatureFields; }
        public void setSignatureFields(int signatureFields) { this.signatureFields = signatureFields; }
        
        public double getCompletionPercentage() { return completionPercentage; }
        public void setCompletionPercentage(double completionPercentage) { this.completionPercentage = completionPercentage; }
    }
    
    // Main class Getters and Setters
    
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    
    public int getFieldCount() { return fieldCount; }
    public void setFieldCount(int fieldCount) { this.fieldCount = fieldCount; }
    
    public List<FormField> getFields() { return fields; }
    public void setFields(List<FormField> fields) { this.fields = fields; }
    
    public Map<String, Object> getFormData() { return formData; }
    public void setFormData(Map<String, Object> formData) { this.formData = formData; }
    
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    
    public String getExportUrl() { return exportUrl; }
    public void setExportUrl(String exportUrl) { this.exportUrl = exportUrl; }
    
    public FormStatistics getStatistics() { return statistics; }
    public void setStatistics(FormStatistics statistics) { this.statistics = statistics; }
    
    public LocalDateTime getExtractedAt() { return extractedAt; }
    public void setExtractedAt(LocalDateTime extractedAt) { this.extractedAt = extractedAt; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
