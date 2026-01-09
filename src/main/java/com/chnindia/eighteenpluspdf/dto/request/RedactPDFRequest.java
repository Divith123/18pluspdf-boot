package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO for redacting sensitive content from a PDF document.
 */
public class RedactPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private List<String> searchTerms;
    private List<RedactionArea> redactionAreas;
    private String redactionColor = "#000000";
    private String overlayText;
    private Boolean removeMetadata = true;
    private Boolean removeComments = true;
    private Boolean removeLinks = false;
    private Boolean removeFormFields = false;
    private String pageRange = "all";
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    
    public List<String> getSearchTerms() { return searchTerms; }
    public void setSearchTerms(List<String> searchTerms) { this.searchTerms = searchTerms; }
    
    public List<RedactionArea> getRedactionAreas() { return redactionAreas; }
    public void setRedactionAreas(List<RedactionArea> redactionAreas) { this.redactionAreas = redactionAreas; }
    
    public String getRedactionColor() { return redactionColor; }
    public void setRedactionColor(String redactionColor) { this.redactionColor = redactionColor; }
    
    public String getOverlayText() { return overlayText; }
    public void setOverlayText(String overlayText) { this.overlayText = overlayText; }
    
    public Boolean getRemoveMetadata() { return removeMetadata; }
    public void setRemoveMetadata(Boolean removeMetadata) { this.removeMetadata = removeMetadata; }
    
    public Boolean getRemoveComments() { return removeComments; }
    public void setRemoveComments(Boolean removeComments) { this.removeComments = removeComments; }
    
    public Boolean getRemoveLinks() { return removeLinks; }
    public void setRemoveLinks(Boolean removeLinks) { this.removeLinks = removeLinks; }
    
    public Boolean getRemoveFormFields() { return removeFormFields; }
    public void setRemoveFormFields(Boolean removeFormFields) { this.removeFormFields = removeFormFields; }
    
    public String getPageRange() { return pageRange; }
    public void setPageRange(String pageRange) { this.pageRange = pageRange; }
    
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
    
    /**
     * Inner class representing a rectangular area to redact
     */
    public static class RedactionArea {
        private int page;
        private float x;
        private float y;
        private float width;
        private float height;
        
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public float getX() { return x; }
        public void setX(float x) { this.x = x; }
        
        public float getY() { return y; }
        public void setY(float y) { this.y = y; }
        
        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }
        
        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }
    }
}
