package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request DTO for filling PDF forms with data.
 * Supports AcroForm fields, XFA forms, and checkbox/radio buttons.
 */
public class FormFillRequest {
    
    @NotNull(message = "Form data is required")
    private Map<String, Object> formData;
    
    private String outputFileName;
    
    private boolean flattenAfterFill = false;
    
    private boolean validateFields = true;
    
    private boolean preserveReadOnly = true;
    
    private String dateFormat = "yyyy-MM-dd";
    
    private String numberFormat = "#,##0.00";
    
    // Getters and Setters
    
    public Map<String, Object> getFormData() {
        return formData;
    }
    
    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public boolean isFlattenAfterFill() {
        return flattenAfterFill;
    }
    
    public void setFlattenAfterFill(boolean flattenAfterFill) {
        this.flattenAfterFill = flattenAfterFill;
    }
    
    public boolean isValidateFields() {
        return validateFields;
    }
    
    public void setValidateFields(boolean validateFields) {
        this.validateFields = validateFields;
    }
    
    public boolean isPreserveReadOnly() {
        return preserveReadOnly;
    }
    
    public void setPreserveReadOnly(boolean preserveReadOnly) {
        this.preserveReadOnly = preserveReadOnly;
    }
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
    
    public String getNumberFormat() {
        return numberFormat;
    }
    
    public void setNumberFormat(String numberFormat) {
        this.numberFormat = numberFormat;
    }
}
