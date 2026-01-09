package com.chnindia.eighteenpluspdf.util;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ValidationUtil {
    
    private static final List<String> ALLOWED_PDF_EXTENSIONS = Arrays.asList("pdf");
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "bmp", "tiff", "tif");
    private static final List<String> ALLOWED_OFFICE_EXTENSIONS = Arrays.asList("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp");
    private static final List<String> ALLOWED_TEXT_EXTENSIONS = Arrays.asList("txt", "rtf", "md", "html");
    
    private static final Pattern PAGE_RANGE_PATTERN = Pattern.compile("^\\d+(-\\d+)?(,\\d+(-\\d+)?)*$|^all$");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^\\d+$");
    private static final Pattern ANGLE_PATTERN = Pattern.compile("^(90|180|270)$");
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    
    /**
     * Validate file is not null and not empty
     */
    public static void validateFileNotNull(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new PDFProcessingException("VALIDATION_ERROR", 
                String.format("%s is required", fieldName), 
                String.format("%s cannot be null or empty", fieldName));
        }
    }
    
    /**
     * Validate file size
     */
    public static void validateFileSize(MultipartFile file, long maxSizeBytes) {
        if (file.getSize() > maxSizeBytes) {
            throw new PDFProcessingException("FILE_TOO_LARGE", 
                String.format("File size %d bytes exceeds maximum %d bytes", file.getSize(), maxSizeBytes),
                "Reduce file size or increase limit");
        }
    }
    
    /**
     * Validate file extension
     */
    public static void validateFileExtension(MultipartFile file, List<String> allowedExtensions) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new PDFProcessingException("VALIDATION_ERROR", "Filename cannot be null");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new PDFProcessingException("UNSUPPORTED_FORMAT", 
                String.format("File extension .%s is not supported", extension),
                "Allowed extensions: " + allowedExtensions);
        }
    }
    
    /**
     * Validate page range format
     */
    public static void validatePageRange(String pageRange, int totalPages) {
        if (pageRange == null || pageRange.trim().isEmpty()) return;
        
        if (!PAGE_RANGE_PATTERN.matcher(pageRange).matches()) {
            throw new PDFProcessingException("INVALID_PAGE_RANGE", 
                "Invalid page range format. Use: 'all', '1-5', '1,3,5', or '1-3,5,7-9'");
        }
        
        if (!pageRange.equals("all")) {
            String[] parts = pageRange.split(",");
            for (String part : parts) {
                if (part.contains("-")) {
                    String[] range = part.split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    if (start < 1 || end > totalPages || start > end) {
                        throw new PDFProcessingException("INVALID_PAGE_RANGE", 
                            String.format("Invalid range %s. Must be between 1 and %d", part, totalPages));
                    }
                } else {
                    int page = Integer.parseInt(part);
                    if (page < 1 || page > totalPages) {
                        throw new PDFProcessingException("INVALID_PAGE_RANGE", 
                            String.format("Page %d is out of range. Must be between 1 and %d", page, totalPages));
                    }
                }
            }
        }
    }
    
    /**
     * Validate rotation angle
     */
    public static void validateRotationAngle(Integer angle) {
        if (angle == null || !ANGLE_PATTERN.matcher(String.valueOf(angle)).matches()) {
            throw new PDFProcessingException("INVALID_ANGLE", 
                "Rotation angle must be 90, 180, or 270 degrees");
        }
    }
    
    /**
     * Validate color format
     */
    public static void validateColor(String color) {
        if (color == null || !COLOR_PATTERN.matcher(color).matches()) {
            throw new PDFProcessingException("INVALID_COLOR", 
                "Color must be in hex format: #RRGGBB (e.g., #FF0000 for red)");
        }
    }
    
    /**
     * Validate opacity
     */
    public static void validateOpacity(Double opacity) {
        if (opacity == null || opacity < 0.0 || opacity > 1.0) {
            throw new PDFProcessingException("INVALID_OPACITY", 
                "Opacity must be between 0.0 (transparent) and 1.0 (opaque)");
        }
    }
    
    /**
     * Validate positive integer
     */
    public static void validatePositiveInteger(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            throw new PDFProcessingException("INVALID_VALUE", 
                String.format("%s must be a positive integer", fieldName));
        }
    }
    
    /**
     * Validate DPI
     */
    public static void validateDPI(Integer dpi) {
        if (dpi == null || dpi < 72 || dpi > 600) {
            throw new PDFProcessingException("INVALID_DPI", 
                "DPI must be between 72 and 600");
        }
    }
    
    /**
     * Validate password strength
     */
    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            throw new PDFProcessingException("WEAK_PASSWORD", 
                "Password must be at least 6 characters long");
        }
    }
    
    /**
     * Validate OCR language
     */
    public static void validateOCRLanguage(String language) {
        if (language == null || language.length() != 3) {
            throw new PDFProcessingException("INVALID_LANGUAGE", 
                "OCR language must be 3-letter code (e.g., 'eng', 'spa', 'fra')");
        }
    }
    
    /**
     * Validate output filename
     */
    public static void validateOutputFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) return;
        
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new PDFProcessingException("INVALID_FILENAME", 
                "Filename cannot contain path separators or parent directory references");
        }
        
        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new PDFProcessingException("INVALID_FILENAME", 
                "Filename can only contain letters, numbers, dots, underscores, and hyphens");
        }
    }
    
    /**
     * Validate compression quality
     */
    public static void validateCompressionQuality(Double quality) {
        if (quality == null || quality < 0.1 || quality > 1.0) {
            throw new PDFProcessingException("INVALID_QUALITY", 
                "Compression quality must be between 0.1 and 1.0");
        }
    }
    
    /**
     * Validate margin
     */
    public static void validateMargin(Integer margin) {
        if (margin == null || margin < 0 || margin > 100) {
            throw new PDFProcessingException("INVALID_MARGIN", 
                "Margin must be between 0 and 100 points");
        }
    }
    
    /**
     * Validate font size
     */
    public static void validateFontSize(Integer fontSize) {
        if (fontSize == null || fontSize < 6 || fontSize > 72) {
            throw new PDFProcessingException("INVALID_FONT_SIZE", 
                "Font size must be between 6 and 72 points");
        }
    }
    
    /**
     * Validate page size
     */
    public static void validatePageSize(String pageSize) {
        if (pageSize == null) return;
        
        List<String> validSizes = Arrays.asList("A4", "A3", "A5", "LETTER", "LEGAL");
        if (!validSizes.contains(pageSize.toUpperCase())) {
            throw new PDFProcessingException("INVALID_PAGE_SIZE", 
                "Page size must be one of: A4, A3, A5, LETTER, LEGAL");
        }
    }
    
    /**
     * Validate compliance level for PDF/A
     */
    public static void validateComplianceLevel(String level) {
        if (level == null) return;
        
        List<String> validLevels = Arrays.asList("1B", "2B", "2U", "3B");
        if (!validLevels.contains(level.toUpperCase())) {
            throw new PDFProcessingException("INVALID_COMPLIANCE", 
                "Compliance level must be one of: 1B, 2B, 2U, 3B");
        }
    }
    
    /**
     * Get file extension
     */
    public static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    /**
     * Get base filename without extension
     */
    public static String getBaseFilename(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf(".");
        return lastDot > 0 ? filename.substring(0, lastDot) : filename;
    }
    
    /**
     * Validate multiple files
     */
    public static void validateMultipleFiles(MultipartFile[] files, int minCount, int maxCount) {
        if (files == null || files.length < minCount) {
            throw new PDFProcessingException("INSUFFICIENT_FILES", 
                String.format("At least %d file(s) required", minCount));
        }
        
        if (files.length > maxCount) {
            throw new PDFProcessingException("TOO_MANY_FILES", 
                String.format("Maximum %d files allowed", maxCount));
        }
        
        for (MultipartFile file : files) {
            validateFileNotNull(file, "File");
        }
    }
    
    /**
     * Validate parameters map
     */
    public static void validateParameters(Object value, String paramName, Class<?> expectedType) {
        if (value == null) return;
        
        if (!expectedType.isInstance(value)) {
            throw new PDFProcessingException("INVALID_PARAMETER", 
                String.format("Parameter %s must be of type %s", paramName, expectedType.getSimpleName()));
        }
    }
}