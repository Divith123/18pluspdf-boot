package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO for adding blank pages to PDFs.
 * Supports various page sizes, positions, and content templates.
 */
public class AddBlankPagesRequest {
    
    @Min(value = 1, message = "At least one page must be added")
    private int count = 1;
    
    @NotNull(message = "Position is required")
    private Position position = Position.END;
    
    private Integer afterPage; // Used when position is AFTER_PAGE
    
    private List<Integer> beforePages; // Insert before specific pages
    
    private String pageSize = "SAME_AS_DOCUMENT"; // A4, Letter, Legal, Custom, SAME_AS_DOCUMENT
    
    private String orientation = "SAME_AS_DOCUMENT"; // PORTRAIT, LANDSCAPE, SAME_AS_DOCUMENT
    
    private float customWidth; // For custom size (in points)
    
    private float customHeight; // For custom size (in points)
    
    private String backgroundColor = "#FFFFFF"; // Hex color or "transparent"
    
    private String outputFileName;
    
    private PageContent pageContent; // Optional content for blank pages
    
    public enum Position {
        START,        // At the beginning
        END,          // At the end
        AFTER_PAGE,   // After specific page number
        BEFORE_PAGES, // Before specific page numbers
        BETWEEN_ALL   // Insert between every page
    }
    
    public static class PageContent {
        private String headerText;
        private String footerText;
        private String centerText;
        private String watermarkText;
        private String templatePath; // Path to template PDF
        private ContentStyle style = new ContentStyle();
        
        // Getters and Setters
        public String getHeaderText() { return headerText; }
        public void setHeaderText(String headerText) { this.headerText = headerText; }
        
        public String getFooterText() { return footerText; }
        public void setFooterText(String footerText) { this.footerText = footerText; }
        
        public String getCenterText() { return centerText; }
        public void setCenterText(String centerText) { this.centerText = centerText; }
        
        public String getWatermarkText() { return watermarkText; }
        public void setWatermarkText(String watermarkText) { this.watermarkText = watermarkText; }
        
        public String getTemplatePath() { return templatePath; }
        public void setTemplatePath(String templatePath) { this.templatePath = templatePath; }
        
        public ContentStyle getStyle() { return style; }
        public void setStyle(ContentStyle style) { this.style = style; }
    }
    
    public static class ContentStyle {
        private String fontName = "Helvetica";
        private float fontSize = 12;
        private String fontColor = "#000000";
        private String alignment = "CENTER"; // LEFT, CENTER, RIGHT
        
        // Getters and Setters
        public String getFontName() { return fontName; }
        public void setFontName(String fontName) { this.fontName = fontName; }
        
        public float getFontSize() { return fontSize; }
        public void setFontSize(float fontSize) { this.fontSize = fontSize; }
        
        public String getFontColor() { return fontColor; }
        public void setFontColor(String fontColor) { this.fontColor = fontColor; }
        
        public String getAlignment() { return alignment; }
        public void setAlignment(String alignment) { this.alignment = alignment; }
    }
    
    // Main class Getters and Setters
    
    public int getCount() {
        return count;
    }
    
    public void setCount(int count) {
        this.count = count;
    }
    
    public Position getPosition() {
        return position;
    }
    
    public void setPosition(Position position) {
        this.position = position;
    }
    
    public Integer getAfterPage() {
        return afterPage;
    }
    
    public void setAfterPage(Integer afterPage) {
        this.afterPage = afterPage;
    }
    
    public List<Integer> getBeforePages() {
        return beforePages;
    }
    
    public void setBeforePages(List<Integer> beforePages) {
        this.beforePages = beforePages;
    }
    
    public String getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }
    
    public String getOrientation() {
        return orientation;
    }
    
    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }
    
    public float getCustomWidth() {
        return customWidth;
    }
    
    public void setCustomWidth(float customWidth) {
        this.customWidth = customWidth;
    }
    
    public float getCustomHeight() {
        return customHeight;
    }
    
    public void setCustomHeight(float customHeight) {
        this.customHeight = customHeight;
    }
    
    public String getBackgroundColor() {
        return backgroundColor;
    }
    
    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public PageContent getPageContent() {
        return pageContent;
    }
    
    public void setPageContent(PageContent pageContent) {
        this.pageContent = pageContent;
    }
}
