package com.chnindia.eighteenpluspdf.dto.request;

import java.util.List;

/**
 * Request DTO for metadata sanitization and privacy cleanup.
 * Removes hidden information, author data, revision history, etc.
 */
public class SanitizeMetadataRequest {
    
    private String outputFileName;
    
    private boolean removeAuthor = true;
    
    private boolean removeTitle = false;
    
    private boolean removeSubject = false;
    
    private boolean removeKeywords = false;
    
    private boolean removeCreator = true;
    
    private boolean removeProducer = true;
    
    private boolean removeCreationDate = true;
    
    private boolean removeModificationDate = true;
    
    private boolean removeTrapped = true;
    
    private boolean removeXMPMetadata = true;
    
    private boolean removeDocumentId = true;
    
    private boolean removeInstanceId = true;
    
    private boolean removeRevisionHistory = true;
    
    private boolean removeAnnotationAuthor = true;
    
    private boolean removeComments = false;
    
    private boolean removeJavaScript = true;
    
    private boolean removeEmbeddedFiles = false;
    
    private boolean removeHiddenLayers = true;
    
    private boolean removeHiddenText = true;
    
    private boolean removeDeletedContent = true;
    
    private boolean removeThumbnails = true;
    
    private boolean removeBookmarkMetadata = false;
    
    private boolean removeLinkedFiles = true;
    
    private boolean removePrivateData = true;
    
    private List<String> customFieldsToRemove; // Custom metadata fields
    
    private SanitizationLevel level = SanitizationLevel.STANDARD;
    
    public enum SanitizationLevel {
        MINIMAL,    // Only critical privacy data
        STANDARD,   // Standard privacy cleanup
        AGGRESSIVE, // Remove all possible metadata
        CUSTOM      // Use individual flags
    }
    
    // Getters and Setters
    
    public String getOutputFileName() {
        return outputFileName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public boolean isRemoveAuthor() {
        return removeAuthor;
    }
    
    public void setRemoveAuthor(boolean removeAuthor) {
        this.removeAuthor = removeAuthor;
    }
    
    public boolean isRemoveTitle() {
        return removeTitle;
    }
    
    public void setRemoveTitle(boolean removeTitle) {
        this.removeTitle = removeTitle;
    }
    
    public boolean isRemoveSubject() {
        return removeSubject;
    }
    
    public void setRemoveSubject(boolean removeSubject) {
        this.removeSubject = removeSubject;
    }
    
    public boolean isRemoveKeywords() {
        return removeKeywords;
    }
    
    public void setRemoveKeywords(boolean removeKeywords) {
        this.removeKeywords = removeKeywords;
    }
    
    public boolean isRemoveCreator() {
        return removeCreator;
    }
    
    public void setRemoveCreator(boolean removeCreator) {
        this.removeCreator = removeCreator;
    }
    
    public boolean isRemoveProducer() {
        return removeProducer;
    }
    
    public void setRemoveProducer(boolean removeProducer) {
        this.removeProducer = removeProducer;
    }
    
    public boolean isRemoveCreationDate() {
        return removeCreationDate;
    }
    
    public void setRemoveCreationDate(boolean removeCreationDate) {
        this.removeCreationDate = removeCreationDate;
    }
    
    public boolean isRemoveModificationDate() {
        return removeModificationDate;
    }
    
    public void setRemoveModificationDate(boolean removeModificationDate) {
        this.removeModificationDate = removeModificationDate;
    }
    
    public boolean isRemoveTrapped() {
        return removeTrapped;
    }
    
    public void setRemoveTrapped(boolean removeTrapped) {
        this.removeTrapped = removeTrapped;
    }
    
    public boolean isRemoveXMPMetadata() {
        return removeXMPMetadata;
    }
    
    public void setRemoveXMPMetadata(boolean removeXMPMetadata) {
        this.removeXMPMetadata = removeXMPMetadata;
    }
    
    public boolean isRemoveDocumentId() {
        return removeDocumentId;
    }
    
    public void setRemoveDocumentId(boolean removeDocumentId) {
        this.removeDocumentId = removeDocumentId;
    }
    
    public boolean isRemoveInstanceId() {
        return removeInstanceId;
    }
    
    public void setRemoveInstanceId(boolean removeInstanceId) {
        this.removeInstanceId = removeInstanceId;
    }
    
    public boolean isRemoveRevisionHistory() {
        return removeRevisionHistory;
    }
    
    public void setRemoveRevisionHistory(boolean removeRevisionHistory) {
        this.removeRevisionHistory = removeRevisionHistory;
    }
    
    public boolean isRemoveAnnotationAuthor() {
        return removeAnnotationAuthor;
    }
    
    public void setRemoveAnnotationAuthor(boolean removeAnnotationAuthor) {
        this.removeAnnotationAuthor = removeAnnotationAuthor;
    }
    
    public boolean isRemoveComments() {
        return removeComments;
    }
    
    public void setRemoveComments(boolean removeComments) {
        this.removeComments = removeComments;
    }
    
    public boolean isRemoveJavaScript() {
        return removeJavaScript;
    }
    
    public void setRemoveJavaScript(boolean removeJavaScript) {
        this.removeJavaScript = removeJavaScript;
    }
    
    public boolean isRemoveEmbeddedFiles() {
        return removeEmbeddedFiles;
    }
    
    public void setRemoveEmbeddedFiles(boolean removeEmbeddedFiles) {
        this.removeEmbeddedFiles = removeEmbeddedFiles;
    }
    
    public boolean isRemoveHiddenLayers() {
        return removeHiddenLayers;
    }
    
    public void setRemoveHiddenLayers(boolean removeHiddenLayers) {
        this.removeHiddenLayers = removeHiddenLayers;
    }
    
    public boolean isRemoveHiddenText() {
        return removeHiddenText;
    }
    
    public void setRemoveHiddenText(boolean removeHiddenText) {
        this.removeHiddenText = removeHiddenText;
    }
    
    public boolean isRemoveDeletedContent() {
        return removeDeletedContent;
    }
    
    public void setRemoveDeletedContent(boolean removeDeletedContent) {
        this.removeDeletedContent = removeDeletedContent;
    }
    
    public boolean isRemoveThumbnails() {
        return removeThumbnails;
    }
    
    public void setRemoveThumbnails(boolean removeThumbnails) {
        this.removeThumbnails = removeThumbnails;
    }
    
    public boolean isRemoveBookmarkMetadata() {
        return removeBookmarkMetadata;
    }
    
    public void setRemoveBookmarkMetadata(boolean removeBookmarkMetadata) {
        this.removeBookmarkMetadata = removeBookmarkMetadata;
    }
    
    public boolean isRemoveLinkedFiles() {
        return removeLinkedFiles;
    }
    
    public void setRemoveLinkedFiles(boolean removeLinkedFiles) {
        this.removeLinkedFiles = removeLinkedFiles;
    }
    
    public boolean isRemovePrivateData() {
        return removePrivateData;
    }
    
    public void setRemovePrivateData(boolean removePrivateData) {
        this.removePrivateData = removePrivateData;
    }
    
    public List<String> getCustomFieldsToRemove() {
        return customFieldsToRemove;
    }
    
    public void setCustomFieldsToRemove(List<String> customFieldsToRemove) {
        this.customFieldsToRemove = customFieldsToRemove;
    }
    
    public SanitizationLevel getLevel() {
        return level;
    }
    
    public void setLevel(SanitizationLevel level) {
        this.level = level;
    }
}
