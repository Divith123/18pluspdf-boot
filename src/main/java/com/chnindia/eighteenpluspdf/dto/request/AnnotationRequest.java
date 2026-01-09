package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for adding annotations to PDFs.
 * Supports highlights, underlines, comments, stamps, drawings, and replies.
 */
public class AnnotationRequest {
    
    @NotEmpty(message = "At least one annotation is required")
    private List<Annotation> annotations;
    
    private String outputFileName;
    
    private String author;
    
    private boolean lockAnnotations = false;
    
    // For removal operations
    private List<String> typesToRemove;
    private List<Integer> pagesToRemove;
    
    // Nested annotation class
    
    public static class Annotation {
        private AnnotationType type;
        private int page;
        private BoundingBox boundingBox;
        private String content;
        private String color = "#FFFF00"; // Yellow default for highlights
        private float opacity = 1.0f;
        private String author;
        private String subject;
        private String replyTo; // For reply threads
        private StampType stampType; // For stamps
        private String customStampPath; // For custom stamps
        private List<Point> points; // For freeform drawings
        private float lineWidth = 1.0f;
        private String lineStyle = "SOLID"; // SOLID, DASHED, DOTTED
        private Map<String, Object> additionalProperties;
        
        public enum AnnotationType {
            HIGHLIGHT,
            UNDERLINE,
            STRIKEOUT,
            SQUIGGLY,
            TEXT_NOTE,
            FREE_TEXT,
            LINE,
            ARROW,
            RECTANGLE,
            CIRCLE,
            POLYGON,
            POLYLINE,
            INK,      // Freehand drawing
            STAMP,
            CARET,
            POPUP,
            FILE_ATTACHMENT,
            SOUND,
            LINK,
            REDACT    // Redaction annotation (not applied until flattened)
        }
        
        public enum StampType {
            APPROVED,
            NOT_APPROVED,
            DRAFT,
            FINAL,
            CONFIDENTIAL,
            FOR_COMMENT,
            FOR_PUBLIC_RELEASE,
            NOT_FOR_PUBLIC_RELEASE,
            VOID,
            AS_IS,
            DEPARTMENTAL,
            EXPERIMENTAL,
            EXPIRED,
            SOLD,
            TOP_SECRET,
            CUSTOM
        }
        
        // Getters and Setters
        public AnnotationType getType() { return type; }
        public void setType(AnnotationType type) { this.type = type; }
        
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        
        public BoundingBox getBoundingBox() { return boundingBox; }
        public void setBoundingBox(BoundingBox boundingBox) { this.boundingBox = boundingBox; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public float getOpacity() { return opacity; }
        public void setOpacity(float opacity) { this.opacity = opacity; }
        
        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        
        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        
        public String getReplyTo() { return replyTo; }
        public void setReplyTo(String replyTo) { this.replyTo = replyTo; }
        
        public StampType getStampType() { return stampType; }
        public void setStampType(StampType stampType) { this.stampType = stampType; }
        
        public String getCustomStampPath() { return customStampPath; }
        public void setCustomStampPath(String customStampPath) { this.customStampPath = customStampPath; }
        
        public List<Point> getPoints() { return points; }
        public void setPoints(List<Point> points) { this.points = points; }
        
        public float getLineWidth() { return lineWidth; }
        public void setLineWidth(float lineWidth) { this.lineWidth = lineWidth; }
        
        public String getLineStyle() { return lineStyle; }
        public void setLineStyle(String lineStyle) { this.lineStyle = lineStyle; }
        
        public Map<String, Object> getAdditionalProperties() { return additionalProperties; }
        public void setAdditionalProperties(Map<String, Object> additionalProperties) { this.additionalProperties = additionalProperties; }
    }
    
    public static class BoundingBox {
        private float x;
        private float y;
        private float width;
        private float height;
        
        public BoundingBox() {}
        
        public BoundingBox(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public float getX() { return x; }
        public void setX(float x) { this.x = x; }
        
        public float getY() { return y; }
        public void setY(float y) { this.y = y; }
        
        public float getWidth() { return width; }
        public void setWidth(float width) { this.width = width; }
        
        public float getHeight() { return height; }
        public void setHeight(float height) { this.height = height; }
    }
    
    public static class Point {
        private float x;
        private float y;
        
        public Point() {}
        
        public Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
        
        public float getX() { return x; }
        public void setX(float x) { this.x = x; }
        
        public float getY() { return y; }
        public void setY(float y) { this.y = y; }
    }
    
    // Main class Getters and Setters
    
    public List<Annotation> getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(List<Annotation> annotations) {
        this.annotations = annotations;
    }
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public boolean isLockAnnotations() {
        return lockAnnotations;
    }
    
    public void setLockAnnotations(boolean lockAnnotations) {
        this.lockAnnotations = lockAnnotations;
    }
    
    public List<String> getTypesToRemove() {
        return typesToRemove;
    }
    
    public void setTypesToRemove(List<String> typesToRemove) {
        this.typesToRemove = typesToRemove;
    }
    
    public List<Integer> getPagesToRemove() {
        return pagesToRemove;
    }
    
    public void setPagesToRemove(List<Integer> pagesToRemove) {
        this.pagesToRemove = pagesToRemove;
    }
}
