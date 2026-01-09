package com.chnindia.eighteenpluspdf.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for PDF comparison operations.
 */
public class CompareResponse {
    
    private String jobId;
    private boolean identical;
    private int totalDifferences;
    private ComparisonSummary summary;
    private List<PageDifference> pageDifferences;
    private String visualDiffUrl;
    private String redlineUrl;
    private String changeListUrl;
    private double similarityScore;
    private LocalDateTime comparedAt;
    private Map<String, Object> metadata;
    
    public static class ComparisonSummary {
        private int pagesInFile1;
        private int pagesInFile2;
        private int identicalPages;
        private int differentPages;
        private int missingPages;
        private int addedPages;
        private int textChanges;
        private int imageChanges;
        private int structuralChanges;
        private int formFieldChanges;
        private int annotationChanges;
        
        // Getters and Setters
        public int getPagesInFile1() { return pagesInFile1; }
        public void setPagesInFile1(int pagesInFile1) { this.pagesInFile1 = pagesInFile1; }
        
        public int getPagesInFile2() { return pagesInFile2; }
        public void setPagesInFile2(int pagesInFile2) { this.pagesInFile2 = pagesInFile2; }
        
        public int getIdenticalPages() { return identicalPages; }
        public void setIdenticalPages(int identicalPages) { this.identicalPages = identicalPages; }
        
        public int getDifferentPages() { return differentPages; }
        public void setDifferentPages(int differentPages) { this.differentPages = differentPages; }
        
        public int getMissingPages() { return missingPages; }
        public void setMissingPages(int missingPages) { this.missingPages = missingPages; }
        
        public int getAddedPages() { return addedPages; }
        public void setAddedPages(int addedPages) { this.addedPages = addedPages; }
        
        public int getTextChanges() { return textChanges; }
        public void setTextChanges(int textChanges) { this.textChanges = textChanges; }
        
        public int getImageChanges() { return imageChanges; }
        public void setImageChanges(int imageChanges) { this.imageChanges = imageChanges; }
        
        public int getStructuralChanges() { return structuralChanges; }
        public void setStructuralChanges(int structuralChanges) { this.structuralChanges = structuralChanges; }
        
        public int getFormFieldChanges() { return formFieldChanges; }
        public void setFormFieldChanges(int formFieldChanges) { this.formFieldChanges = formFieldChanges; }
        
        public int getAnnotationChanges() { return annotationChanges; }
        public void setAnnotationChanges(int annotationChanges) { this.annotationChanges = annotationChanges; }
    }
    
    public static class PageDifference {
        private int pageNumber;
        private DifferenceType type;
        private List<DifferenceRegion> regions;
        private String visualDiffImageUrl;
        private double similarityScore;
        
        public enum DifferenceType {
            IDENTICAL,
            TEXT_CHANGE,
            IMAGE_CHANGE,
            LAYOUT_CHANGE,
            MISSING_IN_FIRST,
            MISSING_IN_SECOND,
            MULTIPLE_CHANGES
        }
        
        // Getters and Setters
        public int getPageNumber() { return pageNumber; }
        public void setPageNumber(int pageNumber) { this.pageNumber = pageNumber; }
        
        public DifferenceType getType() { return type; }
        public void setType(DifferenceType type) { this.type = type; }
        
        public List<DifferenceRegion> getRegions() { return regions; }
        public void setRegions(List<DifferenceRegion> regions) { this.regions = regions; }
        
        public String getVisualDiffImageUrl() { return visualDiffImageUrl; }
        public void setVisualDiffImageUrl(String visualDiffImageUrl) { this.visualDiffImageUrl = visualDiffImageUrl; }
        
        public double getSimilarityScore() { return similarityScore; }
        public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    }
    
    public static class DifferenceRegion {
        private float x;
        private float y;
        private float width;
        private float height;
        private String changeType; // ADDED, REMOVED, MODIFIED
        private String oldContent;
        private String newContent;
        
        // Getters and Setters
        public float getX() { return x; }
        public void setX(float x) { this.x = x; }
        
        public float getY() { return y; }
        public void setY(float y) { this.y = y; }
        
        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }
        
        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }
        
        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }
        
        public String getOldContent() { return oldContent; }
        public void setOldContent(String oldContent) { this.oldContent = oldContent; }
        
        public String getNewContent() { return newContent; }
        public void setNewContent(String newContent) { this.newContent = newContent; }
    }
    
    // Main class Getters and Setters
    
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }
    
    public boolean isIdentical() { return identical; }
    public void setIdentical(boolean identical) { this.identical = identical; }
    
    public int getTotalDifferences() { return totalDifferences; }
    public void setTotalDifferences(int totalDifferences) { this.totalDifferences = totalDifferences; }
    
    public ComparisonSummary getSummary() { return summary; }
    public void setSummary(ComparisonSummary summary) { this.summary = summary; }
    
    public List<PageDifference> getPageDifferences() { return pageDifferences; }
    public void setPageDifferences(List<PageDifference> pageDifferences) { this.pageDifferences = pageDifferences; }
    
    public String getVisualDiffUrl() { return visualDiffUrl; }
    public void setVisualDiffUrl(String visualDiffUrl) { this.visualDiffUrl = visualDiffUrl; }
    
    public String getRedlineUrl() { return redlineUrl; }
    public void setRedlineUrl(String redlineUrl) { this.redlineUrl = redlineUrl; }
    
    public String getChangeListUrl() { return changeListUrl; }
    public void setChangeListUrl(String changeListUrl) { this.changeListUrl = changeListUrl; }
    
    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    
    public LocalDateTime getComparedAt() { return comparedAt; }
    public void setComparedAt(LocalDateTime comparedAt) { this.comparedAt = comparedAt; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
