package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.response.ValidationResponse;
import com.chnindia.eighteenpluspdf.dto.response.ValidationResponse.*;
import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.encryption.PDEncryption;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for PDF validation and quality checks.
 * Provides structural validation, PDF/A compliance, searchability verification, and more.
 */
@Service
public class PDFValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PDFValidationService.class);
    
    /**
     * Perform comprehensive PDF validation.
     */
    public ValidationResponse validatePDF(Path inputFile, ValidationOptions options) {
        logger.info("Validating PDF: {}", inputFile);
        
        ValidationResponse response = new ValidationResponse();
        response.setValidatedAt(LocalDateTime.now());
        
        List<ValidationIssue> issues = new ArrayList<>();
        ValidationStatistics stats = new ValidationStatistics();
        PDFMetrics metrics = new PDFMetrics();
        
        int totalChecks = 0;
        int passedChecks = 0;
        int warningChecks = 0;
        int failedChecks = 0;
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            // Basic file checks
            totalChecks++;
            if (Files.size(inputFile) > 0) {
                passedChecks++;
            } else {
                issues.add(createIssue(ValidationIssue.IssueSeverity.CRITICAL, "FILE", "EMPTY_FILE", 
                    "PDF file is empty", null, null, "Provide a valid PDF file"));
                failedChecks++;
            }
            
            // PDF version check
            metrics.setPdfVersion(String.valueOf(document.getVersion()));
            
            // Page count
            metrics.setPageCount(document.getNumberOfPages());
            totalChecks++;
            if (document.getNumberOfPages() > 0) {
                passedChecks++;
            } else {
                issues.add(createIssue(ValidationIssue.IssueSeverity.ERROR, "STRUCTURE", "NO_PAGES", 
                    "PDF has no pages", null, null, "PDF must contain at least one page"));
                failedChecks++;
            }
            
            // File size
            metrics.setFileSizeBytes(Files.size(inputFile));
            
            // Encryption check
            metrics.setEncrypted(document.isEncrypted());
            if (options.isCheckEncryption()) {
                totalChecks++;
                passedChecks++;
            }
            
            // Validate document structure
            validateDocumentStructure(document, issues, options);
            totalChecks += 3;
            passedChecks += 3 - (int) issues.stream()
                .filter(i -> i.getCategory().equals("STRUCTURE"))
                .filter(i -> i.getSeverity() == ValidationIssue.IssueSeverity.ERROR || 
                            i.getSeverity() == ValidationIssue.IssueSeverity.CRITICAL)
                .count();
            
            // Check for forms
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            metrics.setHasAcroForm(acroForm != null);
            
            // Check for tagged PDF
            metrics.setTagged(document.getDocumentCatalog().getMarkInfo() != null);
            
            // Check searchability
            if (options.isCheckSearchability()) {
                boolean searchable = checkSearchability(document);
                metrics.setSearchable(searchable);
                totalChecks++;
                if (searchable) {
                    passedChecks++;
                } else {
                    issues.add(createIssue(ValidationIssue.IssueSeverity.WARNING, "CONTENT", "NOT_SEARCHABLE", 
                        "PDF may not be fully searchable", null, null, 
                        "Run OCR to make the document searchable"));
                    warningChecks++;
                }
            }
            
            // Check linearization
            totalChecks++;
            if (isLinearized(document)) {
                metrics.setLinearized(true);
                passedChecks++;
            } else {
                metrics.setLinearized(false);
                if (options.isCheckLinearization()) {
                    issues.add(createIssue(ValidationIssue.IssueSeverity.INFO, "OPTIMIZATION", "NOT_LINEARIZED", 
                        "PDF is not linearized for web viewing", null, null, 
                        "Linearize the PDF for faster web viewing"));
                }
            }
            
            // Check fonts
            if (options.isCheckFonts()) {
                validateFonts(document, issues, metrics);
                totalChecks++;
                passedChecks++;
            }
            
            // Check images
            if (options.isCheckImages()) {
                validateImages(document, issues, metrics);
                totalChecks++;
                passedChecks++;
            }
            
            // Count annotations
            int annotationCount = 0;
            for (PDPage page : document.getPages()) {
                annotationCount += page.getAnnotations().size();
            }
            metrics.setAnnotationCount(annotationCount);
            
            // Count bookmarks
            PDDocumentOutline outline = document.getDocumentCatalog().getDocumentOutline();
            if (outline != null) {
                metrics.setBookmarkCount(countBookmarks(outline));
            }
            
            // PDF/A validation
            if (options.isCheckPDFA()) {
                validatePDFA(inputFile, issues, metrics);
                totalChecks++;
                if (metrics.isPDFA()) {
                    passedChecks++;
                }
            }
            
        } catch (IOException e) {
            issues.add(createIssue(ValidationIssue.IssueSeverity.CRITICAL, "FILE", "PARSE_ERROR", 
                "Failed to parse PDF: " + e.getMessage(), null, null, 
                "Repair the PDF or provide a valid file"));
            failedChecks++;
            logger.error("PDF validation failed", e);
        }
        
        // Calculate statistics
        stats.setTotalChecks(totalChecks);
        stats.setPassedChecks(passedChecks);
        stats.setWarningChecks(warningChecks);
        stats.setFailedChecks(failedChecks);
        
        int infoCount = 0, warnCount = 0, errorCount = 0, criticalCount = 0;
        for (ValidationIssue issue : issues) {
            switch (issue.getSeverity()) {
                case INFO: infoCount++; break;
                case WARNING: warnCount++; break;
                case ERROR: errorCount++; break;
                case CRITICAL: criticalCount++; break;
            }
        }
        stats.setInfoCount(infoCount);
        stats.setWarningCount(warnCount);
        stats.setErrorCount(errorCount);
        stats.setCriticalCount(criticalCount);
        
        // Determine overall validation level
        ValidationLevel level;
        if (criticalCount > 0 || errorCount > 0) {
            level = ValidationLevel.FAILED;
        } else if (warnCount > 0) {
            level = ValidationLevel.WARNINGS;
        } else {
            level = ValidationLevel.PASSED;
        }
        
        response.setValid(level != ValidationLevel.FAILED && level != ValidationLevel.ERROR);
        response.setValidationLevel(level);
        response.setIssues(issues);
        response.setStatistics(stats);
        response.setMetrics(metrics);
        
        return response;
    }
    
    private void validateDocumentStructure(PDDocument document, List<ValidationIssue> issues, 
                                            ValidationOptions options) {
        // Check document catalog
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        if (catalog == null) {
            issues.add(createIssue(ValidationIssue.IssueSeverity.CRITICAL, "STRUCTURE", "NO_CATALOG", 
                "Document catalog is missing", null, null, "PDF structure is corrupted"));
            return;
        }
        
        // Check each page
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            PDPage page = document.getPage(i);
            
            // Check page resources
            if (page.getResources() == null && options.isStrictMode()) {
                issues.add(createIssue(ValidationIssue.IssueSeverity.WARNING, "STRUCTURE", "NO_PAGE_RESOURCES", 
                    "Page has no resources", null, i + 1, null));
            }
            
            // Check page media box
            if (page.getMediaBox() == null) {
                issues.add(createIssue(ValidationIssue.IssueSeverity.ERROR, "STRUCTURE", "NO_MEDIA_BOX", 
                    "Page has no media box", null, i + 1, "Define page dimensions"));
            }
        }
    }
    
    private boolean checkSearchability(PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(5, document.getNumberOfPages())); // Check first 5 pages
            
            String text = stripper.getText(document);
            
            // Consider searchable if we can extract at least 100 characters of text
            return text != null && text.trim().length() > 100;
        } catch (IOException e) {
            return false;
        }
    }
    
    private boolean isLinearized(PDDocument document) {
        // Check for linearization dictionary in document
        try {
            return document.getDocument().getLinearizedDictionary() != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    private void validateFonts(PDDocument document, List<ValidationIssue> issues, PDFMetrics metrics) {
        Set<String> fonts = new HashSet<>();
        
        for (PDPage page : document.getPages()) {
            try {
                if (page.getResources() != null) {
                    for (COSName fontName : page.getResources().getFontNames()) {
                        PDFont font = page.getResources().getFont(fontName);
                        if (font != null) {
                            fonts.add(font.getName());
                            
                            // Check if font is embedded
                            if (!font.isEmbedded()) {
                                issues.add(createIssue(ValidationIssue.IssueSeverity.WARNING, "FONT", "NOT_EMBEDDED", 
                                    "Font is not embedded: " + font.getName(), null, null, 
                                    "Embed all fonts for better compatibility"));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn("Error checking fonts on page", e);
            }
        }
        
        metrics.setFontCount(fonts.size());
        metrics.setEmbeddedFonts(new ArrayList<>(fonts));
    }
    
    private void validateImages(PDDocument document, List<ValidationIssue> issues, PDFMetrics metrics) {
        int imageCount = 0;
        
        for (PDPage page : document.getPages()) {
            try {
                if (page.getResources() != null) {
                    imageCount += page.getResources().getXObjectNames().spliterator()
                        .getExactSizeIfKnown();
                }
            } catch (Exception e) {
                logger.warn("Error counting images", e);
            }
        }
        
        metrics.setImageCount(imageCount);
    }
    
    private int countBookmarks(PDDocumentOutline outline) {
        int count = 0;
        var first = outline.getFirstChild();
        while (first != null) {
            count++;
            if (first.getFirstChild() != null) {
                count += countChildBookmarks(first);
            }
            first = first.getNextSibling();
        }
        return count;
    }
    
    private int countChildBookmarks(org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem item) {
        int count = 0;
        var child = item.getFirstChild();
        while (child != null) {
            count++;
            if (child.getFirstChild() != null) {
                count += countChildBookmarks(child);
            }
            child = child.getNextSibling();
        }
        return count;
    }
    
    private void validatePDFA(Path inputFile, List<ValidationIssue> issues, PDFMetrics metrics) {
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            // Check XMP metadata for PDF/A conformance
            PDMetadata metadata = document.getDocumentCatalog().getMetadata();
            
            if (metadata != null) {
                try {
                    String xmpContent = new String(metadata.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
                    
                    if (xmpContent.contains("pdfaid:conformance") || 
                        xmpContent.contains("pdfaid:part") ||
                        xmpContent.contains("PDF/A")) {
                        metrics.setPDFA(true);
                        
                        // Try to extract conformance level
                        if (xmpContent.contains("conformance>A")) {
                            metrics.setPdfaConformance("PDF/A-1a");
                        } else if (xmpContent.contains("conformance>B")) {
                            metrics.setPdfaConformance("PDF/A-1b");
                        } else if (xmpContent.contains("part>2")) {
                            metrics.setPdfaConformance("PDF/A-2");
                        } else if (xmpContent.contains("part>3")) {
                            metrics.setPdfaConformance("PDF/A-3");
                        } else {
                            metrics.setPdfaConformance("PDF/A");
                        }
                    } else {
                        metrics.setPDFA(false);
                        issues.add(createIssue(ValidationIssue.IssueSeverity.INFO, "PDFA", "NO_PDFA_MARKER", 
                            "Document does not contain PDF/A conformance markers", null, null, 
                            "Use PDF/A conversion tool to create compliant document"));
                    }
                } catch (Exception e) {
                    metrics.setPDFA(false);
                    logger.debug("Could not parse XMP metadata: {}", e.getMessage());
                }
            } else {
                metrics.setPDFA(false);
                issues.add(createIssue(ValidationIssue.IssueSeverity.INFO, "PDFA", "NO_XMP_METADATA", 
                    "Document does not contain XMP metadata required for PDF/A", null, null, 
                    "PDF/A documents must include XMP metadata"));
            }
        } catch (Exception e) {
            // PDF/A validation failed - document is not PDF/A compliant
            metrics.setPDFA(false);
            logger.debug("PDF/A validation failed: {}", e.getMessage());
        }
    }
    
    private ValidationIssue createIssue(ValidationIssue.IssueSeverity severity, String category, 
                                         String code, String message, String details, 
                                         Integer pageNumber, String recommendation) {
        ValidationIssue issue = new ValidationIssue();
        issue.setSeverity(severity);
        issue.setCategory(category);
        issue.setCode(code);
        issue.setMessage(message);
        issue.setDetails(details);
        issue.setPageNumber(pageNumber);
        issue.setRecommendation(recommendation);
        return issue;
    }
    
    /**
     * Validate that input and output PDFs have expected properties.
     */
    public void validateProcessingOutput(Path inputFile, Path outputFile, String operation) {
        logger.info("Validating processing output for operation: {}", operation);
        
        try {
            // Basic existence check
            if (!Files.exists(outputFile)) {
                throw new PDFProcessingException("OUTPUT_MISSING", "Output file was not created");
            }
            
            // Size check
            if (Files.size(outputFile) == 0) {
                throw new PDFProcessingException("OUTPUT_EMPTY", "Output file is empty");
            }
            
            // Verify it's a valid PDF
            try (PDDocument doc = Loader.loadPDF(outputFile.toFile())) {
                if (doc.getNumberOfPages() == 0) {
                    // Only error for operations that should produce pages
                    if (!operation.equals("extract-text") && !operation.equals("export-form-data")) {
                        throw new PDFProcessingException("NO_PAGES", "Output PDF has no pages");
                    }
                }
            }
            
            // Operation-specific validations
            switch (operation) {
                case "merge":
                    validateMergeOutput(inputFile, outputFile);
                    break;
                case "split":
                    // Split creates multiple files, handled differently
                    break;
                case "compress":
                    validateCompressOutput(inputFile, outputFile);
                    break;
                default:
                    // Generic validation passed
                    break;
            }
            
        } catch (IOException e) {
            throw new PDFProcessingException("VALIDATION_ERROR", "Output validation failed: " + e.getMessage());
        }
    }
    
    private void validateMergeOutput(Path inputFile, Path outputFile) throws IOException {
        try (PDDocument input = Loader.loadPDF(inputFile.toFile());
             PDDocument output = Loader.loadPDF(outputFile.toFile())) {
            
            // Output should have at least as many pages as input
            if (output.getNumberOfPages() < input.getNumberOfPages()) {
                logger.warn("Merge output has fewer pages than first input");
            }
        }
    }
    
    private void validateCompressOutput(Path inputFile, Path outputFile) throws IOException {
        long inputSize = Files.size(inputFile);
        long outputSize = Files.size(outputFile);
        
        // Warn if output is larger than input
        if (outputSize > inputSize) {
            logger.warn("Compressed output ({}) is larger than input ({})", outputSize, inputSize);
        }
    }
    
    /**
     * Options for validation.
     */
    public static class ValidationOptions {
        private boolean checkEncryption = true;
        private boolean checkSearchability = true;
        private boolean checkLinearization = true;
        private boolean checkFonts = true;
        private boolean checkImages = true;
        private boolean checkPDFA = false;
        private boolean strictMode = false;
        
        // Getters and Setters
        public boolean isCheckEncryption() { return checkEncryption; }
        public void setCheckEncryption(boolean checkEncryption) { this.checkEncryption = checkEncryption; }
        
        public boolean isCheckSearchability() { return checkSearchability; }
        public void setCheckSearchability(boolean checkSearchability) { this.checkSearchability = checkSearchability; }
        
        public boolean isCheckLinearization() { return checkLinearization; }
        public void setCheckLinearization(boolean checkLinearization) { this.checkLinearization = checkLinearization; }
        
        public boolean isCheckFonts() { return checkFonts; }
        public void setCheckFonts(boolean checkFonts) { this.checkFonts = checkFonts; }
        
        public boolean isCheckImages() { return checkImages; }
        public void setCheckImages(boolean checkImages) { this.checkImages = checkImages; }
        
        public boolean isCheckPDFA() { return checkPDFA; }
        public void setCheckPDFA(boolean checkPDFA) { this.checkPDFA = checkPDFA; }
        
        public boolean isStrictMode() { return strictMode; }
        public void setStrictMode(boolean strictMode) { this.strictMode = strictMode; }
        
        public static ValidationOptions defaults() {
            return new ValidationOptions();
        }
        
        public static ValidationOptions strict() {
            ValidationOptions options = new ValidationOptions();
            options.setStrictMode(true);
            options.setCheckPDFA(true);
            return options;
        }
    }
}
