package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request DTO for cloud storage operations.
 * Supports S3, GCS, Azure Blob, Google Drive, OneDrive, Dropbox.
 */
public class CloudStorageRequest {
    
    @NotNull(message = "Cloud provider is required")
    private CloudProvider provider;
    
    @NotNull(message = "Operation is required")
    private Operation operation;
    
    // S3/GCS bucket name
    private String bucket;
    
    // Azure container name
    private String container;
    
    // Azure storage account name
    private String storageAccount;
    
    // Google Drive folder ID
    private String folderId;
    
    // Google Drive/OneDrive file ID
    private String fileId;
    
    // File path prefix for listing
    private String prefix;
    
    private String remotePath;
    
    private String localPath;
    
    private Map<String, String> credentials;
    
    private Map<String, String> metadata;
    
    private boolean publicAccess = false;
    
    private String contentType = "application/pdf";
    
    private String region;
    
    private long expirationMinutes = 60; // For presigned URLs
    
    public enum CloudProvider {
        S3,                 // AWS S3
        GCS,                // Google Cloud Storage
        AZURE_BLOB,         // Azure Blob Storage
        GOOGLE_DRIVE,       // Google Drive
        ONEDRIVE,           // Microsoft OneDrive
        DROPBOX,            // Dropbox
        MINIO               // S3-compatible
    }
    
    public enum Operation {
        UPLOAD,
        DOWNLOAD,
        LIST,
        DELETE,
        GET_PRESIGNED_URL,
        CREATE_FOLDER,
        MOVE,
        COPY,
        GET_METADATA,
        SYNC
    }
    
    // Getters and Setters
    
    public CloudProvider getProvider() {
        return provider;
    }
    
    public void setProvider(CloudProvider provider) {
        this.provider = provider;
    }
    
    public Operation getOperation() {
        return operation;
    }
    
    public void setOperation(Operation operation) {
        this.operation = operation;
    }
    
    public String getBucket() {
        return bucket;
    }
    
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
    
    public String getContainer() {
        return container;
    }
    
    public void setContainer(String container) {
        this.container = container;
    }
    
    public String getStorageAccount() {
        return storageAccount;
    }
    
    public void setStorageAccount(String storageAccount) {
        this.storageAccount = storageAccount;
    }
    
    public String getFolderId() {
        return folderId;
    }
    
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
    
    public String getFileId() {
        return fileId;
    }
    
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public String getRemotePath() {
        return remotePath;
    }
    
    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }
    
    public String getLocalPath() {
        return localPath;
    }
    
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }
    
    public Map<String, String> getCredentials() {
        return credentials;
    }
    
    public void setCredentials(Map<String, String> credentials) {
        this.credentials = credentials;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public boolean isPublicAccess() {
        return publicAccess;
    }
    
    public void setPublicAccess(boolean publicAccess) {
        this.publicAccess = publicAccess;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    public long getExpirationMinutes() {
        return expirationMinutes;
    }
    
    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}
