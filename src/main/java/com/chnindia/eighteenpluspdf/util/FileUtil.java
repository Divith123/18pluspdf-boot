package com.chnindia.eighteenpluspdf.util;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
public class FileUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
    
    @Value("${app.file-storage.temp-dir:./temp}")
    private String tempDir;
    
    @Value("${app.file-storage.output-dir:./data/output}")
    private String outputDir;
    
    @Value("${app.file-storage.allowed-extensions:.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.bmp,.tiff,.txt}")
    private String allowedExtensions;
    
    private static final List<String> PDF_EXTENSIONS = Arrays.asList("pdf");
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "bmp", "tiff", "tif");
    private static final List<String> OFFICE_EXTENSIONS = Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp");
    private static final List<String> TEXT_EXTENSIONS = Arrays.asList("txt", "rtf", "md", "html");
    
    /**
     * Save uploaded file to temporary location
     */
    public Path saveTempFile(MultipartFile file) throws IOException {
        ensureTempDirExists();
        
        String originalName = file.getOriginalFilename();
        String extension = FilenameUtils.getExtension(originalName);
        String baseName = FilenameUtils.getBaseName(originalName);
        
        // Sanitize filename
        String sanitizedName = sanitizeFilename(baseName) + "." + extension;
        String uniqueName = UUID.randomUUID().toString() + "_" + sanitizedName;
        
        Path tempPath = Paths.get(tempDir, uniqueName);
        
        try {
            Files.copy(file.getInputStream(), tempPath);
            logger.info("Saved temp file: {}", tempPath);
            return tempPath;
        } catch (IOException e) {
            logger.error("Failed to save temp file: {}", e.getMessage());
            throw new PDFProcessingException("FILE_SAVE_ERROR", "Failed to save uploaded file", e.getMessage());
        }
    }
    
    /**
     * Save multiple files to temporary location
     */
    public List<Path> saveTempFiles(MultipartFile[] files) throws IOException {
        ensureTempDirExists();
        return Arrays.stream(files)
            .map(this::saveTempFileSafely)
            .toList();
    }
    
    private Path saveTempFileSafely(MultipartFile file) {
        try {
            return saveTempFile(file);
        } catch (IOException e) {
            throw new PDFProcessingException("FILE_SAVE_ERROR", "Failed to save file: " + file.getOriginalFilename(), e.getMessage());
        }
    }
    
    /**
     * Create output file with unique name
     */
    public Path createOutputFile(String baseName, String extension) throws IOException {
        ensureOutputDirExists();
        
        String sanitizedName = sanitizeFilename(baseName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueName = timestamp + "_" + sanitizedName + "." + extension;
        
        return Paths.get(outputDir, uniqueName);
    }
    
    /**
     * Create temporary directory for processing
     */
    public Path createTempDirectory() throws IOException {
        ensureTempDirExists();
        Path tempDirPath = Paths.get(tempDir, "proc_" + UUID.randomUUID().toString());
        Files.createDirectories(tempDirPath);
        return tempDirPath;
    }
    
    /**
     * Calculate file hash for deduplication
     */
    public String calculateFileHash(Path filePath) {
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            logger.warn("Failed to calculate file hash: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Validate file extension
     */
    public boolean isValidExtension(String filename) {
        if (filename == null) return false;
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        String[] allowed = allowedExtensions.split(",");
        return Arrays.stream(allowed)
            .map(String::toLowerCase)
            .map(s -> s.replace(".", ""))
            .anyMatch(ext::equals);
    }
    
    /**
     * Check if a path is safe (no directory traversal)
     */
    public boolean isSafePath(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        // Check for directory traversal attempts
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return false;
        }
        // Check for suspicious characters
        if (filename.contains("\0") || filename.contains("%")) {
            return false;
        }
        // Validate against output directory
        Path normalizedPath = Paths.get(outputDir, filename).normalize();
        Path baseDir = Paths.get(outputDir).normalize();
        return normalizedPath.startsWith(baseDir);
    }
    
    /**
     * Get file type category
     */
    public String getFileType(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        if (PDF_EXTENSIONS.contains(ext)) return "pdf";
        if (IMAGE_EXTENSIONS.contains(ext)) return "image";
        if (OFFICE_EXTENSIONS.contains(ext)) return "office";
        if (TEXT_EXTENSIONS.contains(ext)) return "text";
        return "unknown";
    }
    
    /**
     * Clean up temporary files
     */
    public void cleanupTempFile(Path filePath) {
        if (filePath != null && Files.exists(filePath)) {
            try {
                Files.deleteIfExists(filePath);
                logger.debug("Cleaned up temp file: {}", filePath);
            } catch (IOException e) {
                logger.warn("Failed to cleanup temp file: {}", filePath);
            }
        }
    }
    
    /**
     * Clean up multiple files
     */
    public void cleanupTempFiles(List<Path> filePaths) {
        filePaths.forEach(this::cleanupTempFile);
    }
    
    /**
     * Clean up temporary directory
     */
    public void cleanupTempDirectory(Path dirPath) {
        if (dirPath != null && Files.exists(dirPath)) {
            try {
                FileUtils.deleteDirectory(dirPath.toFile());
                logger.debug("Cleaned up temp directory: {}", dirPath);
            } catch (IOException e) {
                logger.warn("Failed to cleanup temp directory: {}", dirPath);
            }
        }
    }
    
    /**
     * Get file size in human readable format
     */
    public String getHumanReadableSize(Path filePath) {
        try {
            long size = Files.size(filePath);
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
            if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024.0));
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        } catch (IOException e) {
            return "Unknown";
        }
    }
    
    /**
     * Sanitize filename to prevent path traversal
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    
    /**
     * Ensure temp directory exists
     */
    private void ensureTempDirExists() throws IOException {
        Path path = Paths.get(tempDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    /**
     * Ensure output directory exists
     */
    private void ensureOutputDirExists() throws IOException {
        Path path = Paths.get(outputDir);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    
    /**
     * Check if file is PDF
     */
    public boolean isPdfFile(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        return PDF_EXTENSIONS.contains(ext);
    }
    
    /**
     * Check if file is image
     */
    public boolean isImageFile(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        return IMAGE_EXTENSIONS.contains(ext);
    }
    
    /**
     * Check if file is office document
     */
    public boolean isOfficeFile(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        return OFFICE_EXTENSIONS.contains(ext);
    }
    
    /**
     * Get download URL for output file
     */
    public String getDownloadUrl(String filename) {
        return "/api/pdf/download/" + filename;
    }
    
    /**
     * Get base filename without extension
     */
    public String getBaseFilename(String filename) {
        return FilenameUtils.getBaseName(filename);
    }
    
    /**
     * Validate file (static version for backward compatibility)
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PDFProcessingException("INVALID_FILE", "File is required");
        }
        
        if (file.getSize() > 500 * 1024 * 1024) {
            throw new PDFProcessingException("FILE_TOO_LARGE", "File size exceeds 500MB");
        }
        
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().matches(".*\\.(pdf|doc|docx|xls|xlsx|ppt|pptx|jpg|jpeg|png|bmp|tiff|txt)$")) {
            throw new PDFProcessingException("UNSUPPORTED_FORMAT", "Unsupported file format");
        }
    }
    
    public static void cleanupFile(Path filePath) {
        try {
            if (filePath != null && Files.exists(filePath)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log but don't throw - cleanup should be best effort
            System.err.println("Failed to cleanup file: " + filePath + ", error: " + e.getMessage());
        }
    }
    
    public static String getMimeType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }
    
    public static boolean isPDF(Path filePath) {
        String fileName = filePath.toString().toLowerCase();
        return fileName.endsWith(".pdf");
    }
}