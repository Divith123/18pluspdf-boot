package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating PDF forms with field definitions.
 * Supports text fields, checkboxes, radio buttons, dropdowns, signatures.
 */
public class FormCreateRequest {
    
    @NotEmpty(message = "At least one field definition is required")
    private List<FormFieldDefinition> fields;
    
    private String outputFileName;
    
    private PageLayout pageLayout = new PageLayout();
    
    private FormStyle formStyle = new FormStyle();
    
    // Nested classes for field definitions
    
    public static class FormFieldDefinition {
        private String name;
        private FieldType type;
        private float x;
        private float y;
        private float width;
        private float height;
        private int page = 1;
        private String defaultValue;
        private boolean required = false;
        private boolean readOnly = false;
        private boolean multiline = false;
        private int maxLength = -1;
        private List<String> options; // For dropdowns/radio
        private String tooltip;
        private String validationRegex;
        private Map<String, Object> additionalProperties;
        
        public enum FieldType {
            TEXT, CHECKBOX, RADIO, DROPDOWN, LISTBOX, SIGNATURE, DATE, NUMBER, BUTTON
        }
        
        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public FieldType getType() { return type; }
        public void setType(FieldType type) { this.type = type; }
        
        public float getX() { return x; }
        public void setX(float x) { this.x = x; }
        
        public float getY() { return y; }
        public void setY(float y) { this.y = y; }
        
        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }
        
        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }
        
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        
        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }
        
        public boolean isReadOnly() { return readOnly; }
        public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }
        
        public boolean isMultiline() { return multiline; }
        public void setMultiline(boolean multiline) { this.multiline = multiline; }
        
        public int getMaxLength() { return maxLength; }
        public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
        
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        
        public String getTooltip() { return tooltip; }
        public void setTooltip(String tooltip) { this.tooltip = tooltip; }
        
        public String getValidationRegex() { return validationRegex; }
        public void setValidationRegex(String validationRegex) { this.validationRegex = validationRegex; }
        
        public Map<String, Object> getAdditionalProperties() { return additionalProperties; }
        public void setAdditionalProperties(Map<String, Object> additionalProperties) { this.additionalProperties = additionalProperties; }
    }
    
    public static class PageLayout {
        private String pageSize = "A4"; // A4, Letter, Legal, Custom
        private String orientation = "PORTRAIT"; // PORTRAIT, LANDSCAPE
        private float customWidth;
        private float customHeight;
        private float marginTop = 72;
        private float marginBottom = 72;
        private float marginLeft = 72;
        private float marginRight = 72;
        
        // Getters and Setters
        public String getPageSize() { return pageSize; }
        public void setPageSize(String pageSize) { this.pageSize = pageSize; }
        
        public String getOrientation() { return orientation; }
        public void setOrientation(String orientation) { this.orientation = orientation; }
        
        public float getCustomWidth() { return customWidth; }
        public void setCustomWidth(float customWidth) { this.customWidth = customWidth; }
        
        public float getCustomHeight() { return customHeight; }
        public void setCustomHeight(float customHeight) { this.customHeight = customHeight; }
        
        public float getMarginTop() { return marginTop; }
        public void setMarginTop(float marginTop) { this.marginTop = marginTop; }
        
        public float getMarginBottom() { return marginBottom; }
        public void setMarginBottom(float marginBottom) { this.marginBottom = marginBottom; }
        
        public float getMarginLeft() { return marginLeft; }
        public void setMarginLeft(float marginLeft) { this.marginLeft = marginLeft; }
        
        public float getMarginRight() { return marginRight; }
        public void setMarginRight(float marginRight) { this.marginRight = marginRight; }
    }
    
    public static class FormStyle {
        private String fontName = "Helvetica";
        private float fontSize = 12;
        private String fontColor = "#000000";
        private String backgroundColor = "#FFFFFF";
        private String borderColor = "#000000";
        private float borderWidth = 1;
        private boolean showLabels = true;
        
        // Getters and Setters
        public String getFontName() { return fontName; }
        public void setFontName(String fontName) { this.fontName = fontName; }
        
        public float getFontSize() { return fontSize; }
        public void setFontSize(float fontSize) { this.fontSize = fontSize; }
        
        public String getFontColor() { return fontColor; }
        public void setFontColor(String fontColor) { this.fontColor = fontColor; }
        
        public String getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }
        
        public String getBorderColor() { return borderColor; }
        public void setBorderColor(String borderColor) { this.borderColor = borderColor; }
        
        public float getBorderWidth() { return borderWidth; }
        public void setBorderWidth(float borderWidth) { this.borderWidth = borderWidth; }
        
        public boolean isShowLabels() { return showLabels; }
        public void setShowLabels(boolean showLabels) { this.showLabels = showLabels; }
    }
    
    // Main class Getters and Setters
    
    public List<FormFieldDefinition> getFields() {
        return fields;
    }
    
    public void setFields(List<FormFieldDefinition> fields) {
        this.fields = fields;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public PageLayout getPageLayout() {
        return pageLayout;
    }
    
    public void setPageLayout(PageLayout pageLayout) {
        this.pageLayout = pageLayout;
    }
    
    public FormStyle getFormStyle() {
        return formStyle;
    }
    
    public void setFormStyle(FormStyle formStyle) {
        this.formStyle = formStyle;
    }
}
