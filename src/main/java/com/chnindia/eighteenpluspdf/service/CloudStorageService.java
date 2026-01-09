package com.chnindia.eighteenpluspdf.service;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.sas.*;
import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.*;
import java.util.*;

/**
 * Production-Ready Cloud Storage Service with REAL SDK implementations.
 * This service provides actual cloud storage functionality using official SDKs.
 * 
 * Implementations:
 * - AWS S3: Full implementation using AWS SDK v2
 * - Google Cloud Storage: Full implementation using GCS client
 * - Azure Blob Storage: Full implementation using Azure SDK
 * - Dropbox: Full implementation using Dropbox SDK v2
 * 
 * Note: Import statements for SDK classes are conditional based on
 * what's available at compile time. Enable specific providers by
 * uncommenting relevant code sections and adding dependencies.
 */
@Service
public class CloudStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudStorageService.class);
    
    @Value("${cloud.aws.access-key-id:}")
    private String awsAccessKeyId;
    
    @Value("${cloud.aws.secret-access-key:}")
    private String awsSecretAccessKey;
    
    @Value("${cloud.aws.region:us-east-1}")
    private String awsRegion;
    
    @Value("${cloud.gcs.project-id:}")
    private String gcsProjectId;
    
    @Value("${cloud.gcs.credentials-path:}")
    private String gcsCredentialsPath;
    
    @Value("${cloud.azure.connection-string:}")
    private String azureConnectionString;
    
    @Value("${cloud.dropbox.access-token:}")
    private String dropboxAccessToken;
    
    // Client instances - lazily initialized
    private Object s3Client;  // S3Client
    private Object s3Presigner; // S3Presigner
    private Object gcsStorage; // com.google.cloud.storage.Storage
    private BlobServiceClient azureBlobClient;
    private Object dropboxClient; // DbxClientV2
    
    private boolean s3Initialized = false;
    private boolean gcsInitialized = false;
    private boolean azureInitialized = false;
    private boolean dropboxInitialized = false;
    
    @PostConstruct
    public void initialize() {
        logger.info("Initializing Real Cloud Storage Service");
        
        // Initialize providers based on available configuration
        if (isNotEmpty(awsAccessKeyId) && isNotEmpty(awsSecretAccessKey)) {
            initializeS3();
        }
        
        if (isNotEmpty(gcsCredentialsPath) || isNotEmpty(gcsProjectId)) {
            initializeGCS();
        }
        
        if (isNotEmpty(azureConnectionString)) {
            initializeAzure();
        }
        
        if (isNotEmpty(dropboxAccessToken)) {
            initializeDropbox();
        }
        
        logInitializationStatus();
    }
    
    // ==================== AWS S3 IMPLEMENTATION ====================
    
    private void initializeS3() {
        try {
            // Using reflection to avoid compile-time dependency issues
            // In real implementation, use direct imports:
            /*
            import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
            import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
            import software.amazon.awssdk.regions.Region;
            import software.amazon.awssdk.services.s3.S3Client;
            import software.amazon.awssdk.services.s3.presigner.S3Presigner;
            
            AwsBasicCredentials credentials = AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey);
            s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
            
            s3Presigner = S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
            */
            
            Class<?> awsCredsClass = Class.forName("software.amazon.awssdk.auth.credentials.AwsBasicCredentials");
            Class<?> staticProviderClass = Class.forName("software.amazon.awssdk.auth.credentials.StaticCredentialsProvider");
            Class<?> regionClass = Class.forName("software.amazon.awssdk.regions.Region");
            Class<?> s3ClientClass = Class.forName("software.amazon.awssdk.services.s3.S3Client");
            Class<?> s3PresignerClass = Class.forName("software.amazon.awssdk.services.s3.presigner.S3Presigner");
            
            // Create credentials
            Object credentials = awsCredsClass.getMethod("create", String.class, String.class)
                .invoke(null, awsAccessKeyId, awsSecretAccessKey);
            Object provider = staticProviderClass.getMethod("create", 
                Class.forName("software.amazon.awssdk.auth.credentials.AwsCredentials"))
                .invoke(null, credentials);
            Object region = regionClass.getMethod("of", String.class).invoke(null, awsRegion);
            
            // Build S3Client
            Object s3Builder = s3ClientClass.getMethod("builder").invoke(null);
            s3Builder = s3Builder.getClass().getMethod("region", regionClass).invoke(s3Builder, region);
            s3Builder = s3Builder.getClass().getMethod("credentialsProvider", 
                Class.forName("software.amazon.awssdk.auth.credentials.AwsCredentialsProvider"))
                .invoke(s3Builder, provider);
            s3Client = s3Builder.getClass().getMethod("build").invoke(s3Builder);
            
            // Build S3Presigner
            Object presignerBuilder = s3PresignerClass.getMethod("builder").invoke(null);
            presignerBuilder = presignerBuilder.getClass().getMethod("region", regionClass)
                .invoke(presignerBuilder, region);
            presignerBuilder = presignerBuilder.getClass().getMethod("credentialsProvider", 
                Class.forName("software.amazon.awssdk.auth.credentials.AwsCredentialsProvider"))
                .invoke(presignerBuilder, provider);
            s3Presigner = presignerBuilder.getClass().getMethod("build").invoke(presignerBuilder);
            
            s3Initialized = true;
            logger.info("✅ AWS S3 initialized successfully (Region: {})", awsRegion);
            
        } catch (ClassNotFoundException e) {
            logger.warn("AWS SDK not available. Add 'software.amazon.awssdk:s3' dependency.");
        } catch (Exception e) {
            logger.error("Failed to initialize AWS S3: {}", e.getMessage());
        }
    }
    
    /**
     * Upload file to AWS S3.
     */
    public Map<String, Object> uploadToS3(Path localFile, String bucket, String key) {
        ensureS3Initialized();
        
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            /*
            // Direct implementation (use when AWS SDK is on classpath):
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(Files.probeContentType(localFile))
                .build();
            
            s3Client.putObject(request, RequestBody.fromFile(localFile));
            */
            
            Class<?> putObjectRequestClass = Class.forName("software.amazon.awssdk.services.s3.model.PutObjectRequest");
            Class<?> requestBodyClass = Class.forName("software.amazon.awssdk.core.sync.RequestBody");
            
            // Build request
            Object requestBuilder = putObjectRequestClass.getMethod("builder").invoke(null);
            requestBuilder = requestBuilder.getClass().getMethod("bucket", String.class)
                .invoke(requestBuilder, bucket);
            requestBuilder = requestBuilder.getClass().getMethod("key", String.class)
                .invoke(requestBuilder, key);
            
            String contentType = Files.probeContentType(localFile);
            if (contentType != null) {
                requestBuilder = requestBuilder.getClass().getMethod("contentType", String.class)
                    .invoke(requestBuilder, contentType);
            }
            
            Object request = requestBuilder.getClass().getMethod("build").invoke(requestBuilder);
            
            // Create RequestBody
            Object requestBody = requestBodyClass.getMethod("fromFile", Path.class).invoke(null, localFile);
            
            // Execute upload
            s3Client.getClass().getMethod("putObject", putObjectRequestClass, requestBodyClass)
                .invoke(s3Client, request, requestBody);
            
            long fileSize = Files.size(localFile);
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("provider", "S3");
            result.put("bucket", bucket);
            result.put("key", key);
            result.put("region", awsRegion);
            result.put("size", fileSize);
            result.put("sizeFormatted", formatFileSize(fileSize));
            result.put("url", String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, awsRegion, key));
            result.put("durationMs", duration);
            result.put("throughputMBps", fileSize / (1024.0 * 1024.0) / (duration / 1000.0));
            
            logger.info("✅ Uploaded to S3: s3://{}/{} ({} in {}ms)", bucket, key, 
                formatFileSize(fileSize), duration);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("❌ S3 upload failed: {}", e.getMessage());
            throw new PDFProcessingException("S3_UPLOAD_ERROR", "S3 upload failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Download file from AWS S3.
     */
    public Path downloadFromS3(String bucket, String key, Path targetDir) {
        ensureS3Initialized();
        
        try {
            String fileName = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
            Path targetFile = targetDir.resolve(fileName);
            
            /*
            // Direct implementation:
            GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
            
            s3Client.getObject(request, ResponseTransformer.toFile(targetFile));
            */
            
            Class<?> getObjectRequestClass = Class.forName("software.amazon.awssdk.services.s3.model.GetObjectRequest");
            Class<?> responseTransformerClass = Class.forName("software.amazon.awssdk.core.sync.ResponseTransformer");
            
            Object requestBuilder = getObjectRequestClass.getMethod("builder").invoke(null);
            requestBuilder = requestBuilder.getClass().getMethod("bucket", String.class)
                .invoke(requestBuilder, bucket);
            requestBuilder = requestBuilder.getClass().getMethod("key", String.class)
                .invoke(requestBuilder, key);
            Object request = requestBuilder.getClass().getMethod("build").invoke(requestBuilder);
            
            Object transformer = responseTransformerClass.getMethod("toFile", Path.class)
                .invoke(null, targetFile);
            
            s3Client.getClass().getMethod("getObject", getObjectRequestClass, responseTransformerClass)
                .invoke(s3Client, request, transformer);
            
            logger.info("✅ Downloaded from S3: s3://{}/{} -> {}", bucket, key, targetFile);
            return targetFile;
            
        } catch (Exception e) {
            logger.error("❌ S3 download failed: {}", e.getMessage());
            throw new PDFProcessingException("S3_DOWNLOAD_ERROR", "S3 download failed: " + e.getMessage());
        }
    }
    
    /**
     * List objects in S3 bucket.
     */
    public List<Map<String, Object>> listS3Objects(String bucket, String prefix) {
        ensureS3Initialized();
        
        List<Map<String, Object>> objects = new ArrayList<>();
        
        try {
            Class<?> listRequestClass = Class.forName("software.amazon.awssdk.services.s3.model.ListObjectsV2Request");
            Class<?> listResponseClass = Class.forName("software.amazon.awssdk.services.s3.model.ListObjectsV2Response");
            
            Object requestBuilder = listRequestClass.getMethod("builder").invoke(null);
            requestBuilder = requestBuilder.getClass().getMethod("bucket", String.class)
                .invoke(requestBuilder, bucket);
            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder = requestBuilder.getClass().getMethod("prefix", String.class)
                    .invoke(requestBuilder, prefix);
            }
            Object request = requestBuilder.getClass().getMethod("build").invoke(requestBuilder);
            
            Object response = s3Client.getClass().getMethod("listObjectsV2", listRequestClass)
                .invoke(s3Client, request);
            
            @SuppressWarnings("unchecked")
            List<Object> contents = (List<Object>) listResponseClass.getMethod("contents").invoke(response);
            
            for (Object s3Object : contents) {
                Map<String, Object> obj = new LinkedHashMap<>();
                obj.put("key", s3Object.getClass().getMethod("key").invoke(s3Object));
                obj.put("size", s3Object.getClass().getMethod("size").invoke(s3Object));
                obj.put("lastModified", s3Object.getClass().getMethod("lastModified").invoke(s3Object).toString());
                obj.put("storageClass", s3Object.getClass().getMethod("storageClassAsString").invoke(s3Object));
                objects.add(obj);
            }
            
            logger.info("Listed {} objects from S3: s3://{}/{}", objects.size(), bucket, prefix);
            
        } catch (Exception e) {
            logger.error("❌ S3 list failed: {}", e.getMessage());
            throw new PDFProcessingException("S3_LIST_ERROR", "S3 list failed: " + e.getMessage());
        }
        
        return objects;
    }
    
    /**
     * Generate presigned URL for S3 object.
     */
    public String generateS3PresignedUrl(String bucket, String key, int expirationMinutes) {
        ensureS3Initialized();
        
        try {
            /*
            // Direct implementation:
            GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getRequest)
                .build();
            
            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
            */
            
            Class<?> getObjectRequestClass = Class.forName("software.amazon.awssdk.services.s3.model.GetObjectRequest");
            Class<?> presignRequestClass = Class.forName("software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest");
            
            Object getBuilder = getObjectRequestClass.getMethod("builder").invoke(null);
            getBuilder = getBuilder.getClass().getMethod("bucket", String.class).invoke(getBuilder, bucket);
            getBuilder = getBuilder.getClass().getMethod("key", String.class).invoke(getBuilder, key);
            Object getRequest = getBuilder.getClass().getMethod("build").invoke(getBuilder);
            
            Object presignBuilder = presignRequestClass.getMethod("builder").invoke(null);
            presignBuilder = presignBuilder.getClass().getMethod("signatureDuration", Duration.class)
                .invoke(presignBuilder, Duration.ofMinutes(expirationMinutes));
            presignBuilder = presignBuilder.getClass().getMethod("getObjectRequest", getObjectRequestClass)
                .invoke(presignBuilder, getRequest);
            Object presignRequest = presignBuilder.getClass().getMethod("build").invoke(presignBuilder);
            
            Object presigned = s3Presigner.getClass().getMethod("presignGetObject", presignRequestClass)
                .invoke(s3Presigner, presignRequest);
            Object url = presigned.getClass().getMethod("url").invoke(presigned);
            
            String urlString = url.toString();
            logger.info("Generated presigned URL for s3://{}/{} (expires in {} min)", bucket, key, expirationMinutes);
            
            return urlString;
            
        } catch (Exception e) {
            logger.error("❌ S3 presign failed: {}", e.getMessage());
            throw new PDFProcessingException("S3_PRESIGN_ERROR", "S3 presign failed: " + e.getMessage());
        }
    }
    
    /**
     * Delete object from S3.
     */
    public boolean deleteFromS3(String bucket, String key) {
        ensureS3Initialized();
        
        try {
            Class<?> deleteRequestClass = Class.forName("software.amazon.awssdk.services.s3.model.DeleteObjectRequest");
            
            Object requestBuilder = deleteRequestClass.getMethod("builder").invoke(null);
            requestBuilder = requestBuilder.getClass().getMethod("bucket", String.class)
                .invoke(requestBuilder, bucket);
            requestBuilder = requestBuilder.getClass().getMethod("key", String.class)
                .invoke(requestBuilder, key);
            Object request = requestBuilder.getClass().getMethod("build").invoke(requestBuilder);
            
            s3Client.getClass().getMethod("deleteObject", deleteRequestClass).invoke(s3Client, request);
            
            logger.info("✅ Deleted from S3: s3://{}/{}", bucket, key);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ S3 delete failed: {}", e.getMessage());
            return false;
        }
    }
    
    // ==================== GOOGLE CLOUD STORAGE IMPLEMENTATION ====================
    
    private void initializeGCS() {
        try {
            /*
            // Direct implementation:
            import com.google.cloud.storage.Storage;
            import com.google.cloud.storage.StorageOptions;
            import com.google.auth.oauth2.GoogleCredentials;
            
            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new FileInputStream(gcsCredentialsPath));
            gcsStorage = StorageOptions.newBuilder()
                .setProjectId(gcsProjectId)
                .setCredentials(credentials)
                .build()
                .getService();
            */
            
            Class<?> storageOptionsClass = Class.forName("com.google.cloud.storage.StorageOptions");
            
            Object builder = storageOptionsClass.getMethod("newBuilder").invoke(null);
            
            if (isNotEmpty(gcsProjectId)) {
                builder = builder.getClass().getMethod("setProjectId", String.class)
                    .invoke(builder, gcsProjectId);
            }
            
            if (isNotEmpty(gcsCredentialsPath)) {
                Class<?> googleCredsClass = Class.forName("com.google.auth.oauth2.GoogleCredentials");
                try (FileInputStream fis = new FileInputStream(gcsCredentialsPath)) {
                    Object credentials = googleCredsClass.getMethod("fromStream", InputStream.class)
                        .invoke(null, fis);
                    builder = builder.getClass().getMethod("setCredentials", 
                        Class.forName("com.google.auth.Credentials"))
                        .invoke(builder, credentials);
                }
            }
            
            Object options = builder.getClass().getMethod("build").invoke(builder);
            gcsStorage = options.getClass().getMethod("getService").invoke(options);
            
            gcsInitialized = true;
            logger.info("✅ Google Cloud Storage initialized (Project: {})", gcsProjectId);
            
        } catch (ClassNotFoundException e) {
            logger.warn("Google Cloud Storage SDK not available. Add 'com.google.cloud:google-cloud-storage' dependency.");
        } catch (Exception e) {
            logger.error("Failed to initialize GCS: {}", e.getMessage());
        }
    }
    
    /**
     * Upload file to Google Cloud Storage.
     */
    public Map<String, Object> uploadToGCS(Path localFile, String bucket, String objectName) {
        ensureGCSInitialized();
        
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            Class<?> blobIdClass = Class.forName("com.google.cloud.storage.BlobId");
            Class<?> blobInfoClass = Class.forName("com.google.cloud.storage.BlobInfo");
            
            // BlobId.of(bucket, objectName)
            Object blobId = blobIdClass.getMethod("of", String.class, String.class)
                .invoke(null, bucket, objectName);
            
            // BlobInfo.newBuilder(blobId).setContentType(...).build()
            Object blobInfoBuilder = blobInfoClass.getMethod("newBuilder", blobIdClass).invoke(null, blobId);
            String contentType = Files.probeContentType(localFile);
            if (contentType != null) {
                blobInfoBuilder = blobInfoBuilder.getClass().getMethod("setContentType", String.class)
                    .invoke(blobInfoBuilder, contentType);
            }
            Object blobInfo = blobInfoBuilder.getClass().getMethod("build").invoke(blobInfoBuilder);
            
            // storage.create(blobInfo, Files.readAllBytes(localFile))
            byte[] content = Files.readAllBytes(localFile);
            gcsStorage.getClass().getMethod("create", blobInfoClass, byte[].class)
                .invoke(gcsStorage, blobInfo, content);
            
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("provider", "GCS");
            result.put("bucket", bucket);
            result.put("object", objectName);
            result.put("size", content.length);
            result.put("sizeFormatted", formatFileSize(content.length));
            result.put("url", String.format("gs://%s/%s", bucket, objectName));
            result.put("publicUrl", String.format("https://storage.googleapis.com/%s/%s", bucket, objectName));
            result.put("durationMs", duration);
            
            logger.info("✅ Uploaded to GCS: gs://{}/{}", bucket, objectName);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new PDFProcessingException("GCS_UPLOAD_ERROR", "GCS upload failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Download file from Google Cloud Storage.
     */
    public Path downloadFromGCS(String bucket, String objectName, Path targetDir) {
        ensureGCSInitialized();
        
        try {
            String fileName = objectName.contains("/") ? 
                objectName.substring(objectName.lastIndexOf('/') + 1) : objectName;
            Path targetFile = targetDir.resolve(fileName);
            
            Class<?> blobIdClass = Class.forName("com.google.cloud.storage.BlobId");
            Class<?> blobClass = Class.forName("com.google.cloud.storage.Blob");
            
            Object blobId = blobIdClass.getMethod("of", String.class, String.class)
                .invoke(null, bucket, objectName);
            
            Object blob = gcsStorage.getClass().getMethod("get", blobIdClass).invoke(gcsStorage, blobId);
            
            if (blob == null) {
                throw new PDFProcessingException("GCS_NOT_FOUND", 
                    "Object not found: gs://" + bucket + "/" + objectName);
            }
            
            byte[] content = (byte[]) blobClass.getMethod("getContent").invoke(blob);
            Files.write(targetFile, content);
            
            logger.info("✅ Downloaded from GCS: gs://{}/{} -> {}", bucket, objectName, targetFile);
            return targetFile;
            
        } catch (Exception e) {
            throw new PDFProcessingException("GCS_DOWNLOAD_ERROR", "GCS download failed: " + e.getMessage());
        }
    }
    
    // ==================== AZURE BLOB STORAGE IMPLEMENTATION ====================
    
    private void initializeAzure() {
        try {
            azureBlobClient = new BlobServiceClientBuilder()
                .connectionString(azureConnectionString)
                .buildClient();
            
            azureInitialized = true;
            logger.info("✅ Azure Blob Storage initialized");
            
        } catch (Exception e) {
            logger.error("Failed to initialize Azure Blob: {}", e.getMessage());
        }
    }
    
    /**
     * Upload file to Azure Blob Storage.
     */
    public Map<String, Object> uploadToAzure(Path localFile, String container, String blobName) {
        ensureAzureInitialized();
        
        Map<String, Object> result = new LinkedHashMap<>();
        long startTime = System.currentTimeMillis();
        
        try {
            BlobContainerClient containerClient = azureBlobClient.getBlobContainerClient(container);
            
            // Create container if not exists
            if (!containerClient.exists()) {
                containerClient.create();
            }
            
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            // Upload with overwrite
            blobClient.uploadFromFile(localFile.toString(), true);
            
            long fileSize = Files.size(localFile);
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("success", true);
            result.put("provider", "AZURE");
            result.put("container", container);
            result.put("blob", blobName);
            result.put("size", fileSize);
            result.put("sizeFormatted", formatFileSize(fileSize));
            result.put("url", blobClient.getBlobUrl());
            result.put("durationMs", duration);
            
            logger.info("✅ Uploaded to Azure: {}/{}", container, blobName);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new PDFProcessingException("AZURE_UPLOAD_ERROR", "Azure upload failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Download file from Azure Blob Storage.
     */
    public Path downloadFromAzure(String container, String blobName, Path targetDir) {
        ensureAzureInitialized();
        
        try {
            String fileName = blobName.contains("/") ? 
                blobName.substring(blobName.lastIndexOf('/') + 1) : blobName;
            Path targetFile = targetDir.resolve(fileName);
            
            BlobContainerClient containerClient = azureBlobClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            blobClient.downloadToFile(targetFile.toString(), true);
            
            logger.info("✅ Downloaded from Azure: {}/{} -> {}", container, blobName, targetFile);
            return targetFile;
            
        } catch (Exception e) {
            throw new PDFProcessingException("AZURE_DOWNLOAD_ERROR", "Azure download failed: " + e.getMessage());
        }
    }
    
    /**
     * List blobs in Azure container.
     */
    public List<Map<String, Object>> listAzureBlobs(String container, String prefix) {
        ensureAzureInitialized();
        
        List<Map<String, Object>> blobs = new ArrayList<>();
        
        try {
            BlobContainerClient containerClient = azureBlobClient.getBlobContainerClient(container);
            
            ListBlobsOptions options = new ListBlobsOptions();
            if (prefix != null && !prefix.isEmpty()) {
                options.setPrefix(prefix);
            }
            
            for (BlobItem blob : containerClient.listBlobs(options, null)) {
                Map<String, Object> blobInfo = new LinkedHashMap<>();
                blobInfo.put("name", blob.getName());
                blobInfo.put("size", blob.getProperties().getContentLength());
                blobInfo.put("lastModified", blob.getProperties().getLastModified().toString());
                blobInfo.put("contentType", blob.getProperties().getContentType());
                blobInfo.put("accessTier", blob.getProperties().getAccessTier() != null ? 
                    blob.getProperties().getAccessTier().toString() : "Hot");
                blobs.add(blobInfo);
            }
            
            logger.info("Listed {} blobs from Azure container: {}", blobs.size(), container);
            
        } catch (Exception e) {
            throw new PDFProcessingException("AZURE_LIST_ERROR", "Azure list failed: " + e.getMessage());
        }
        
        return blobs;
    }
    
    /**
     * Generate SAS URL for Azure blob.
     */
    public String generateAzureSasUrl(String container, String blobName, int expirationMinutes) {
        ensureAzureInitialized();
        
        try {
            BlobContainerClient containerClient = azureBlobClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(expirationMinutes);
            
            BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            
            BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(expiryTime, permission);
            
            String sasToken = blobClient.generateSas(sasValues);
            String sasUrl = blobClient.getBlobUrl() + "?" + sasToken;
            
            logger.info("Generated SAS URL for Azure blob: {}/{}", container, blobName);
            return sasUrl;
            
        } catch (Exception e) {
            throw new PDFProcessingException("AZURE_SAS_ERROR", "Azure SAS generation failed: " + e.getMessage());
        }
    }
    
    /**
     * Delete blob from Azure.
     */
    public boolean deleteFromAzure(String container, String blobName) {
        ensureAzureInitialized();
        
        try {
            BlobContainerClient containerClient = azureBlobClient.getBlobContainerClient(container);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            
            blobClient.delete();
            
            logger.info("✅ Deleted from Azure: {}/{}", container, blobName);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Azure delete failed: {}", e.getMessage());
            return false;
        }
    }
    
    // ==================== DROPBOX IMPLEMENTATION ====================
    
    private void initializeDropbox() {
        try {
            /*
            // Direct implementation:
            import com.dropbox.core.DbxRequestConfig;
            import com.dropbox.core.v2.DbxClientV2;
            
            DbxRequestConfig config = DbxRequestConfig.newBuilder("pdf-platform").build();
            dropboxClient = new DbxClientV2(config, dropboxAccessToken);
            */
            
            Class<?> configClass = Class.forName("com.dropbox.core.DbxRequestConfig");
            Class<?> clientClass = Class.forName("com.dropbox.core.v2.DbxClientV2");
            
            Object configBuilder = configClass.getMethod("newBuilder", String.class)
                .invoke(null, "pdf-platform");
            Object config = configBuilder.getClass().getMethod("build").invoke(configBuilder);
            
            dropboxClient = clientClass.getConstructor(configClass, String.class)
                .newInstance(config, dropboxAccessToken);
            
            dropboxInitialized = true;
            logger.info("✅ Dropbox initialized");
            
        } catch (ClassNotFoundException e) {
            logger.warn("Dropbox SDK not available. Add 'com.dropbox.core:dropbox-core-sdk' dependency.");
        } catch (Exception e) {
            logger.error("Failed to initialize Dropbox: {}", e.getMessage());
        }
    }
    
    /**
     * Upload file to Dropbox.
     */
    public Map<String, Object> uploadToDropbox(Path localFile, String dropboxPath) {
        ensureDropboxInitialized();
        
        Map<String, Object> result = new LinkedHashMap<>();
        
        try {
            /*
            // Direct implementation:
            try (InputStream in = Files.newInputStream(localFile)) {
                FileMetadata metadata = dropboxClient.files()
                    .uploadBuilder(dropboxPath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
                    
                result.put("id", metadata.getId());
                result.put("path", metadata.getPathDisplay());
                result.put("size", metadata.getSize());
            }
            */
            
            Object filesClient = dropboxClient.getClass().getMethod("files").invoke(dropboxClient);
            
            try (InputStream in = Files.newInputStream(localFile)) {
                Object uploadBuilder = filesClient.getClass().getMethod("uploadBuilder", String.class)
                    .invoke(filesClient, dropboxPath);
                
                Class<?> writeModeClass = Class.forName("com.dropbox.core.v2.files.WriteMode");
                Object overwriteMode = writeModeClass.getField("OVERWRITE").get(null);
                uploadBuilder = uploadBuilder.getClass().getMethod("withMode", writeModeClass)
                    .invoke(uploadBuilder, overwriteMode);
                
                Object metadata = uploadBuilder.getClass().getMethod("uploadAndFinish", InputStream.class)
                    .invoke(uploadBuilder, in);
                
                result.put("success", true);
                result.put("provider", "DROPBOX");
                result.put("id", metadata.getClass().getMethod("getId").invoke(metadata));
                result.put("path", metadata.getClass().getMethod("getPathDisplay").invoke(metadata));
                result.put("size", metadata.getClass().getMethod("getSize").invoke(metadata));
            }
            
            logger.info("✅ Uploaded to Dropbox: {}", dropboxPath);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            throw new PDFProcessingException("DROPBOX_UPLOAD_ERROR", "Dropbox upload failed: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Download file from Dropbox.
     */
    public Path downloadFromDropbox(String dropboxPath, Path targetDir) {
        ensureDropboxInitialized();
        
        try {
            String fileName = dropboxPath.contains("/") ? 
                dropboxPath.substring(dropboxPath.lastIndexOf('/') + 1) : dropboxPath;
            Path targetFile = targetDir.resolve(fileName);
            
            Object filesClient = dropboxClient.getClass().getMethod("files").invoke(dropboxClient);
            
            try (OutputStream out = Files.newOutputStream(targetFile)) {
                Object downloader = filesClient.getClass().getMethod("download", String.class)
                    .invoke(filesClient, dropboxPath);
                downloader.getClass().getMethod("download", OutputStream.class).invoke(downloader, out);
            }
            
            logger.info("✅ Downloaded from Dropbox: {} -> {}", dropboxPath, targetFile);
            return targetFile;
            
        } catch (Exception e) {
            throw new PDFProcessingException("DROPBOX_DOWNLOAD_ERROR", "Dropbox download failed: " + e.getMessage());
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private void ensureS3Initialized() {
        if (!s3Initialized) {
            throw new PDFProcessingException("S3_NOT_CONFIGURED", 
                "AWS S3 not initialized. Set cloud.aws.access-key-id and cloud.aws.secret-access-key in configuration.");
        }
    }
    
    private void ensureGCSInitialized() {
        if (!gcsInitialized) {
            throw new PDFProcessingException("GCS_NOT_CONFIGURED", 
                "Google Cloud Storage not initialized. Set cloud.gcs.project-id and cloud.gcs.credentials-path in configuration.");
        }
    }
    
    private void ensureAzureInitialized() {
        if (!azureInitialized) {
            throw new PDFProcessingException("AZURE_NOT_CONFIGURED", 
                "Azure Blob Storage not initialized. Set cloud.azure.connection-string in configuration.");
        }
    }
    
    private void ensureDropboxInitialized() {
        if (!dropboxInitialized) {
            throw new PDFProcessingException("DROPBOX_NOT_CONFIGURED", 
                "Dropbox not initialized. Set cloud.dropbox.access-token in configuration.");
        }
    }
    
    private boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    private void logInitializationStatus() {
        logger.info("=== Cloud Storage Initialization Status ===");
        logger.info("  AWS S3:        {}", s3Initialized ? "✅ READY" : "❌ Not configured");
        logger.info("  GCS:           {}", gcsInitialized ? "✅ READY" : "❌ Not configured");
        logger.info("  Azure Blob:    {}", azureInitialized ? "✅ READY" : "❌ Not configured");
        logger.info("  Dropbox:       {}", dropboxInitialized ? "✅ READY" : "❌ Not configured");
        logger.info("==========================================");
    }
    
    /**
     * Get status of all cloud providers.
     */
    public Map<String, Object> getProviderStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        
        status.put("s3", Map.of(
            "initialized", s3Initialized,
            "region", awsRegion,
            "configured", isNotEmpty(awsAccessKeyId)
        ));
        
        status.put("gcs", Map.of(
            "initialized", gcsInitialized,
            "projectId", gcsProjectId != null ? gcsProjectId : "",
            "configured", isNotEmpty(gcsProjectId) || isNotEmpty(gcsCredentialsPath)
        ));
        
        status.put("azure", Map.of(
            "initialized", azureInitialized,
            "configured", isNotEmpty(azureConnectionString)
        ));
        
        status.put("dropbox", Map.of(
            "initialized", dropboxInitialized,
            "configured", isNotEmpty(dropboxAccessToken)
        ));
        
        return status;
    }
}
