package com.chnindia.eighteenpluspdf.dto.request;

import java.util.List;

/**
 * Request DTO for bookmark (outline) management in PDFs.
 * Supports creating, editing, extracting, and auto-generating bookmarks.
 */
public class BookmarkRequest {
    
    private Operation operation = Operation.ADD;
    
    private List<BookmarkEntry> bookmarks;
    
    private AutoGenerateOptions autoGenerateOptions;
    
    private String outputFileName;
    
    private boolean replaceExisting = false;
    
    private boolean expandAll = true;
    
    // Additional fields for bookmark operations
    private boolean preserveExisting = true;
    private boolean removeAll = false;
    private List<String> titlesToRemove;
    private PageRange pageRangeToRemove;
    private boolean detectNumberedHeadings = true;
    
    // Type alias for BookmarkDefinition
    public static class BookmarkDefinition extends BookmarkEntry {
        // Inherits all fields from BookmarkEntry
    }
    
    // Page range for removing bookmarks
    public static class PageRange {
        private int startPage;
        private int endPage;
        
        public int getStartPage() { return startPage; }
        public void setStartPage(int startPage) { this.startPage = startPage; }
        
        public int getEndPage() { return endPage; }
        public void setEndPage(int endPage) { this.endPage = endPage; }
    }
    
    public enum Operation {
        ADD,            // Add new bookmarks
        REMOVE,         // Remove specific bookmarks
        REMOVE_ALL,     // Remove all bookmarks
        EXTRACT,        // Extract bookmarks as JSON/XML
        UPDATE,         // Update existing bookmarks
        AUTO_GENERATE   // Auto-generate from headings/pages
    }
    
    public static class BookmarkEntry {
        private String title;
        private int pageNumber;
        private Integer pagePosition; // Y position on page (optional)
        private String action; // GoTo, URI, JavaScript
        private String uri; // For URI action
        private List<BookmarkEntry> children; // Nested bookmarks
        private boolean expanded = true;
        private String color; // Hex color
        private boolean bold = false;
        private boolean italic = false;
        
        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        
        public Integer getPagePosition() { return pagePosition; }
        public void setPagePosition(Integer pagePosition) { this.pagePosition = pagePosition; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getUri() { return uri; }
        public void setUri(String uri) { this.uri = uri; }
        
        public List<BookmarkEntry> getChildren() { return children; }
        public void setChildren(List<BookmarkEntry> children) { this.children = children; }
        
        public boolean isExpanded() { return expanded; }
        public void setExpanded(boolean expanded) { this.expanded = expanded; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }
        
        public boolean isItalic() { return italic; }
        public void setItalic(boolean italic) { this.italic = italic; }
    }
    
    public static class AutoGenerateOptions {
        private GenerateSource source = GenerateSource.PAGE_TITLES;
        private List<String> headingPatterns; // Regex patterns for detecting headings
        private List<Integer> headingFontSizes; // Font sizes to consider as headings
        private int maxDepth = 3; // Maximum nesting depth
        private String namingTemplate = "{filename}"; // For file-based naming
        private boolean detectChapters = true;
        private boolean detectSections = true;
        private boolean detectNumberedHeadings = true;
        private List<String> excludePatterns; // Text patterns to exclude
        
        public enum GenerateSource {
            PAGE_TITLES,     // From page titles/headers
            TEXT_HEADINGS,   // From detected text headings
            FILE_NAMES,      // From merged file names
            PAGE_NUMBERS,    // One bookmark per page
            OCR_HEADINGS     // From OCR-detected headings
        }
        
        // Getters and Setters
        public GenerateSource getSource() { return source; }
        public void setSource(GenerateSource source) { this.source = source; }
        
        public List<String> getHeadingPatterns() { return headingPatterns; }
        public void setHeadingPatterns(List<String> headingPatterns) { this.headingPatterns = headingPatterns; }
        
        public List<Integer> getHeadingFontSizes() { return headingFontSizes; }
        public void setHeadingFontSizes(List<Integer> headingFontSizes) { this.headingFontSizes = headingFontSizes; }
        
        public int getMaxDepth() { return maxDepth; }
        public void setMaxDepth(int maxDepth) { this.maxDepth = maxDepth; }
        
        public String getNamingTemplate() { return namingTemplate; }
        public void setNamingTemplate(String namingTemplate) { this.namingTemplate = namingTemplate; }
        
        public boolean isDetectChapters() { return detectChapters; }
        public void setDetectChapters(boolean detectChapters) { this.detectChapters = detectChapters; }
        
        public boolean isDetectSections() { return detectSections; }
        public void setDetectSections(boolean detectSections) { this.detectSections = detectSections; }
        
        public boolean isDetectNumberedHeadings() { return detectNumberedHeadings; }
        public void setDetectNumberedHeadings(boolean detectNumberedHeadings) { this.detectNumberedHeadings = detectNumberedHeadings; }
        
        public List<String> getExcludePatterns() { return excludePatterns; }
        public void setExcludePatterns(List<String> excludePatterns) { this.excludePatterns = excludePatterns; }
    }
    
    // Main class Getters and Setters
    
    public Operation getOperation() {
        return operation;
    }
    
    public void setOperation(Operation operation) {
        this.operation = operation;
    }
    
    public List<BookmarkEntry> getBookmarks() {
        return bookmarks;
    }
    
    public void setBookmarks(List<BookmarkEntry> bookmarks) {
        this.bookmarks = bookmarks;
    }
    
    public AutoGenerateOptions getAutoGenerateOptions() {
        return autoGenerateOptions;
    }
    
    public void setAutoGenerateOptions(AutoGenerateOptions autoGenerateOptions) {
        this.autoGenerateOptions = autoGenerateOptions;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public boolean isReplaceExisting() {
        return replaceExisting;
    }
    
    public void setReplaceExisting(boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }
    
    public boolean isExpandAll() {
        return expandAll;
    }
    
    public void setExpandAll(boolean expandAll) {
        this.expandAll = expandAll;
    }
    
    public boolean isPreserveExisting() {
        return preserveExisting;
    }
    
    public void setPreserveExisting(boolean preserveExisting) {
        this.preserveExisting = preserveExisting;
    }
    
    public boolean isRemoveAll() {
        return removeAll;
    }
    
    public void setRemoveAll(boolean removeAll) {
        this.removeAll = removeAll;
    }
    
    public List<String> getTitlesToRemove() {
        return titlesToRemove;
    }
    
    public void setTitlesToRemove(List<String> titlesToRemove) {
        this.titlesToRemove = titlesToRemove;
    }
    
    public PageRange getPageRangeToRemove() {
        return pageRangeToRemove;
    }
    
    public void setPageRangeToRemove(PageRange pageRangeToRemove) {
        this.pageRangeToRemove = pageRangeToRemove;
    }
    
    public boolean isDetectNumberedHeadings() {
        return detectNumberedHeadings;
    }
    
    public void setDetectNumberedHeadings(boolean detectNumberedHeadings) {
        this.detectNumberedHeadings = detectNumberedHeadings;
    }
}
