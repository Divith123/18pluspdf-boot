package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.interactive.action.*;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Metadata Sanitization and Hidden Data Removal Service.
 * 
 * Comprehensive removal of sensitive/hidden data from PDFs including:
 * - Document metadata (title, author, creator, etc.)
 * - XMP metadata streams
 * - Document history/incremental saves
 * - Hidden text and layers
 * - JavaScript and actions
 * - Embedded files and attachments
 * - Comments and annotations
 * - Personal information patterns
 * - Thumbnail images
 * - Form field data
 * - Custom metadata
 * - Producer/creator application info
 */
@Service
public class MetadataSanitizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetadataSanitizationService.class);
    
    @Autowired
    private FileUtil fileUtil;
    
    // Patterns for detecting personal information
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\+?[0-9]{1,4}[-.\\s]?\\(?[0-9]{1,3}\\)?[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,4}[-.\\s]?[0-9]{1,9}");
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    
    /**
     * Perform comprehensive sanitization based on options.
     */
    public SanitizationResult sanitize(Path inputFile, Path outputFile, SanitizationOptions options) {
        logger.info("Sanitizing PDF: {} with options: {}", inputFile, options);
        
        SanitizationResult result = new SanitizationResult();
        result.setInputFile(inputFile.toString());
        long startTime = System.currentTimeMillis();
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            
            // 1. Remove document metadata
            if (options.isRemoveMetadata()) {
                int removed = removeDocumentMetadata(document);
                result.addRemovedItem("documentMetadata", removed);
            }
            
            // 2. Remove XMP metadata
            if (options.isRemoveXmpMetadata()) {
                boolean removed = removeXmpMetadata(document);
                result.addRemovedItem("xmpMetadata", removed ? 1 : 0);
            }
            
            // 3. Remove JavaScript
            if (options.isRemoveJavaScript()) {
                int removed = removeJavaScript(document);
                result.addRemovedItem("javaScript", removed);
            }
            
            // 4. Remove embedded files/attachments
            if (options.isRemoveAttachments()) {
                int removed = removeAttachments(document);
                result.addRemovedItem("attachments", removed);
            }
            
            // 5. Remove annotations (comments, markups)
            if (options.isRemoveAnnotations()) {
                int removed = removeAnnotations(document);
                result.addRemovedItem("annotations", removed);
            }
            
            // 6. Remove form field data
            if (options.isRemoveFormData()) {
                int removed = removeFormData(document);
                result.addRemovedItem("formFields", removed);
            }
            
            // 7. Remove document links and actions
            if (options.isRemoveLinks()) {
                int removed = removeLinksAndActions(document);
                result.addRemovedItem("linksAndActions", removed);
            }
            
            // 8. Remove thumbnails
            if (options.isRemoveThumbnails()) {
                int removed = removeThumbnails(document);
                result.addRemovedItem("thumbnails", removed);
            }
            
            // 9. Remove bookmarks/outlines
            if (options.isRemoveBookmarks()) {
                boolean removed = removeBookmarks(document);
                result.addRemovedItem("bookmarks", removed ? 1 : 0);
            }
            
            // 10. Remove hidden layers
            if (options.isRemoveHiddenLayers()) {
                int removed = removeHiddenLayers(document);
                result.addRemovedItem("hiddenLayers", removed);
            }
            
            // 11. Remove private application data
            if (options.isRemovePrivateData()) {
                int removed = removePrivateApplicationData(document);
                result.addRemovedItem("privateAppData", removed);
            }
            
            // 12. Clear document history (save as new)
            if (options.isClearHistory()) {
                result.addRemovedItem("incrementalSaves", 1);
                // Saving without incremental update clears history
            }
            
            // 13. Scan and report PII (optional redaction)
            if (options.isScanForPII()) {
                List<PIIFinding> piiFindings = scanForPII(document);
                result.setPiiFindings(piiFindings);
            }
            
            // Save sanitized document
            document.setAllSecurityToBeRemoved(true); // Remove any encryption for clean save
            document.save(outputFile.toFile());
            
            result.setOutputFile(outputFile.toString());
            result.setSuccess(true);
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            result.setInputSize(Files.size(inputFile));
            result.setOutputSize(Files.size(outputFile));
            
            logger.info("Sanitization complete: {} items removed", result.getTotalItemsRemoved());
            
        } catch (Exception e) {
            logger.error("Sanitization failed", e);
            result.setSuccess(false);
            result.setError(e.getMessage());
            throw new PDFProcessingException("SANITIZATION_ERROR", "Failed to sanitize PDF: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Analyze document for hidden/sensitive data without removing.
     */
    public SanitizationAnalysis analyze(Path inputFile) {
        logger.info("Analyzing PDF for hidden data: {}", inputFile);
        
        SanitizationAnalysis analysis = new SanitizationAnalysis();
        analysis.setFilePath(inputFile.toString());
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            
            // Check document metadata
            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                Map<String, String> metadata = new LinkedHashMap<>();
                if (info.getTitle() != null) metadata.put("title", info.getTitle());
                if (info.getAuthor() != null) metadata.put("author", info.getAuthor());
                if (info.getSubject() != null) metadata.put("subject", info.getSubject());
                if (info.getKeywords() != null) metadata.put("keywords", info.getKeywords());
                if (info.getCreator() != null) metadata.put("creator", info.getCreator());
                if (info.getProducer() != null) metadata.put("producer", info.getProducer());
                if (info.getCreationDate() != null) metadata.put("creationDate", info.getCreationDate().getTime().toString());
                if (info.getModificationDate() != null) metadata.put("modificationDate", info.getModificationDate().getTime().toString());
                
                // Check custom metadata
                for (String key : info.getMetadataKeys()) {
                    if (!metadata.containsKey(key)) {
                        String value = info.getCustomMetadataValue(key);
                        if (value != null) metadata.put(key, value);
                    }
                }
                
                analysis.setDocumentMetadata(metadata);
                analysis.setHasMetadata(!metadata.isEmpty());
            }
            
            // Check XMP metadata
            PDMetadata xmp = document.getDocumentCatalog().getMetadata();
            analysis.setHasXmpMetadata(xmp != null);
            
            // Check JavaScript
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDDocumentNameDictionary names = catalog.getNames();
            analysis.setHasJavaScript(names != null && names.getJavaScript() != null);
            
            // Check embedded files
            int attachmentCount = 0;
            if (names != null && names.getEmbeddedFiles() != null) {
                try {
                    var efTree = names.getEmbeddedFiles();
                    if (efTree.getNames() != null) {
                        attachmentCount = efTree.getNames().size();
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            analysis.setAttachmentCount(attachmentCount);
            
            // Check annotations
            int annotationCount = 0;
            for (PDPage page : document.getPages()) {
                List<PDAnnotation> annotations = page.getAnnotations();
                if (annotations != null) {
                    annotationCount += annotations.size();
                }
            }
            analysis.setAnnotationCount(annotationCount);
            
            // Check form fields
            PDAcroForm acroForm = catalog.getAcroForm();
            int formFieldCount = 0;
            if (acroForm != null && acroForm.getFields() != null) {
                formFieldCount = acroForm.getFields().size();
            }
            analysis.setFormFieldCount(formFieldCount);
            
            // Check bookmarks
            analysis.setHasBookmarks(catalog.getDocumentOutline() != null);
            
            // Check thumbnails
            int thumbnailCount = 0;
            for (PDPage page : document.getPages()) {
                if (page.getCOSObject().containsKey(COSName.THUMB)) {
                    thumbnailCount++;
                }
            }
            analysis.setThumbnailCount(thumbnailCount);
            
            // Check for hidden layers (OCGs)
            int hiddenLayerCount = 0;
            // OCG check would require deeper analysis
            analysis.setHiddenLayerCount(hiddenLayerCount);
            
            // Scan for PII
            List<PIIFinding> piiFindings = scanForPII(document);
            analysis.setPiiFindings(piiFindings);
            analysis.setHasPotentialPII(!piiFindings.isEmpty());
            
            // Calculate risk score
            int riskScore = calculateRiskScore(analysis);
            analysis.setRiskScore(riskScore);
            analysis.setRiskLevel(getRiskLevel(riskScore));
            
            // Generate recommendations
            List<String> recommendations = generateRecommendations(analysis);
            analysis.setRecommendations(recommendations);
            
        } catch (Exception e) {
            logger.error("Analysis failed", e);
            analysis.setError(e.getMessage());
        }
        
        return analysis;
    }
    
    // ==================== REMOVAL METHODS ====================
    
    private int removeDocumentMetadata(PDDocument document) {
        int count = 0;
        PDDocumentInformation info = document.getDocumentInformation();
        
        if (info != null) {
            if (info.getTitle() != null) { info.setTitle(null); count++; }
            if (info.getAuthor() != null) { info.setAuthor(null); count++; }
            if (info.getSubject() != null) { info.setSubject(null); count++; }
            if (info.getKeywords() != null) { info.setKeywords(null); count++; }
            if (info.getCreator() != null) { info.setCreator(null); count++; }
            if (info.getProducer() != null) { info.setProducer(null); count++; }
            info.setCreationDate(null);
            info.setModificationDate(null);
            
            // Remove custom metadata
            Set<String> customKeys = new HashSet<>(info.getMetadataKeys());
            for (String key : customKeys) {
                info.setCustomMetadataValue(key, null);
                count++;
            }
        }
        
        return count;
    }
    
    private boolean removeXmpMetadata(PDDocument document) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        if (catalog.getMetadata() != null) {
            catalog.setMetadata(null);
            return true;
        }
        return false;
    }
    
    private int removeJavaScript(PDDocument document) {
        int count = 0;
        
        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDDocumentNameDictionary names = catalog.getNames();
            
            if (names != null) {
                // Remove JavaScript name tree
                COSDictionary namesDictionary = names.getCOSObject();
                if (namesDictionary.containsKey(COSName.JAVA_SCRIPT)) {
                    namesDictionary.removeItem(COSName.JAVA_SCRIPT);
                    count++;
                }
            }
            
            // Remove OpenAction if it's JavaScript (using COS level to avoid API differences)
            COSDictionary catalogDict = catalog.getCOSObject();
            if (catalogDict.containsKey(COSName.OPEN_ACTION)) {
                COSBase openAction = catalogDict.getDictionaryObject(COSName.OPEN_ACTION);
                if (openAction instanceof COSDictionary) {
                    COSDictionary actionDict = (COSDictionary) openAction;
                    COSName type = actionDict.getCOSName(COSName.S);
                    if (type != null && "JavaScript".equals(type.getName())) {
                        catalogDict.removeItem(COSName.OPEN_ACTION);
                        count++;
                    }
                }
            }
            
            // Remove page-level JavaScript actions
            for (PDPage page : document.getPages()) {
                PDPageAdditionalActions actions = page.getActions();
                if (actions != null) {
                    page.setActions(null);
                    count++;
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error removing JavaScript: {}", e.getMessage());
        }
        
        return count;
    }
    
    private int removeAttachments(PDDocument document) {
        int count = 0;
        
        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            PDDocumentNameDictionary names = catalog.getNames();
            
            if (names != null) {
                var efTree = names.getEmbeddedFiles();
                if (efTree != null && efTree.getNames() != null) {
                    count = efTree.getNames().size();
                    
                    // Remove embedded files
                    COSDictionary namesDictionary = names.getCOSObject();
                    namesDictionary.removeItem(COSName.EMBEDDED_FILES);
                }
            }
            
            // Remove file attachments from annotations
            for (PDPage page : document.getPages()) {
                List<PDAnnotation> annotations = page.getAnnotations();
                List<PDAnnotation> filtered = new ArrayList<>();
                
                for (PDAnnotation annot : annotations) {
                    if (!"FileAttachment".equals(annot.getSubtype())) {
                        filtered.add(annot);
                    } else {
                        count++;
                    }
                }
                
                page.setAnnotations(filtered);
            }
            
        } catch (Exception e) {
            logger.warn("Error removing attachments: {}", e.getMessage());
        }
        
        return count;
    }
    
    private int removeAnnotations(PDDocument document) {
        int count = 0;
        
        try {
            for (PDPage page : document.getPages()) {
                List<PDAnnotation> annotations = page.getAnnotations();
                if (annotations != null && !annotations.isEmpty()) {
                    count += annotations.size();
                    page.setAnnotations(new ArrayList<>());
                }
            }
        } catch (Exception e) {
            logger.warn("Error removing annotations: {}", e.getMessage());
        }
        
        return count;
    }
    
    private int removeFormData(PDDocument document) {
        int count = 0;
        
        try {
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null && acroForm.getFields() != null) {
                for (var field : acroForm.getFields()) {
                    try {
                        field.setValue(null);
                        count++;
                    } catch (Exception e) {
                        // Some fields may not support setValue
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Error removing form data: {}", e.getMessage());
        }
        
        return count;
    }
    
    private int removeLinksAndActions(PDDocument document) {
        int count = 0;
        
        try {
            for (PDPage page : document.getPages()) {
                List<PDAnnotation> annotations = page.getAnnotations();
                List<PDAnnotation> filtered = new ArrayList<>();
                
                for (PDAnnotation annot : annotations) {
                    String subtype = annot.getSubtype();
                    if (!"Link".equals(subtype) && !"Widget".equals(subtype)) {
                        filtered.add(annot);
                    } else {
                        count++;
                    }
                }
                
                page.setAnnotations(filtered);
            }
            
            // Remove document-level actions using COS-level API
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            COSDictionary catalogDict = catalog.getCOSObject();
            if (catalogDict.containsKey(COSName.OPEN_ACTION)) {
                catalogDict.removeItem(COSName.OPEN_ACTION);
            }
            
        } catch (Exception e) {
            logger.warn("Error removing links: {}", e.getMessage());
        }
        
        return count;
    }
    
    private int removeThumbnails(PDDocument document) {
        int count = 0;
        
        for (PDPage page : document.getPages()) {
            COSDictionary pageDictionary = page.getCOSObject();
            if (pageDictionary.containsKey(COSName.THUMB)) {
                pageDictionary.removeItem(COSName.THUMB);
                count++;
            }
        }
        
        return count;
    }
    
    private boolean removeBookmarks(PDDocument document) {
        PDDocumentCatalog catalog = document.getDocumentCatalog();
        if (catalog.getDocumentOutline() != null) {
            catalog.setDocumentOutline(null);
            return true;
        }
        return false;
    }
    
    private int removeHiddenLayers(PDDocument document) {
        int count = 0;
        
        try {
            PDDocumentCatalog catalog = document.getDocumentCatalog();
            COSDictionary catalogDict = catalog.getCOSObject();
            
            // Remove OCProperties (Optional Content Groups/Layers) - use getPDFName for custom names
            COSName ocPropertiesName = COSName.getPDFName("OCProperties");
            if (catalogDict.containsKey(ocPropertiesName)) {
                catalogDict.removeItem(ocPropertiesName);
                count++;
            }
        } catch (Exception e) {
            logger.warn("Error removing hidden layers: {}", e.getMessage());
        }
        
        return count;
    }
    
    private int removePrivateApplicationData(PDDocument document) {
        int count = 0;
        
        try {
            COSDictionary catalogDict = document.getDocumentCatalog().getCOSObject();
            
            // Remove PieceInfo (application-private data)
            if (catalogDict.containsKey(COSName.PIECE_INFO)) {
                catalogDict.removeItem(COSName.PIECE_INFO);
                count++;
            }
            
            // Remove from pages too
            for (PDPage page : document.getPages()) {
                COSDictionary pageDict = page.getCOSObject();
                if (pageDict.containsKey(COSName.PIECE_INFO)) {
                    pageDict.removeItem(COSName.PIECE_INFO);
                    count++;
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error removing private data: {}", e.getMessage());
        }
        
        return count;
    }
    
    // ==================== PII SCANNING ====================
    
    private List<PIIFinding> scanForPII(PDDocument document) {
        List<PIIFinding> findings = new ArrayList<>();
        
        try {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            
            // Scan for emails
            var emailMatcher = EMAIL_PATTERN.matcher(text);
            while (emailMatcher.find()) {
                findings.add(new PIIFinding("EMAIL", emailMatcher.group(), "Document text"));
            }
            
            // Scan for phone numbers
            var phoneMatcher = PHONE_PATTERN.matcher(text);
            while (phoneMatcher.find()) {
                String phone = phoneMatcher.group();
                if (phone.replaceAll("[^0-9]", "").length() >= 10) {
                    findings.add(new PIIFinding("PHONE", maskPII(phone), "Document text"));
                }
            }
            
            // Scan for SSN patterns
            var ssnMatcher = SSN_PATTERN.matcher(text);
            while (ssnMatcher.find()) {
                findings.add(new PIIFinding("SSN", "***-**-" + ssnMatcher.group().substring(7), "Document text"));
            }
            
            // Scan for credit card patterns
            var ccMatcher = CREDIT_CARD_PATTERN.matcher(text);
            while (ccMatcher.find()) {
                String cc = ccMatcher.group().replaceAll("[^0-9]", "");
                if (cc.length() >= 13 && cc.length() <= 19 && isValidLuhn(cc)) {
                    findings.add(new PIIFinding("CREDIT_CARD", maskPII(ccMatcher.group()), "Document text"));
                }
            }
            
            // Scan metadata for personal info
            PDDocumentInformation info = document.getDocumentInformation();
            if (info != null) {
                if (info.getAuthor() != null && !info.getAuthor().isEmpty()) {
                    findings.add(new PIIFinding("AUTHOR_NAME", info.getAuthor(), "Metadata"));
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error scanning for PII: {}", e.getMessage());
        }
        
        return findings;
    }
    
    private String maskPII(String value) {
        if (value == null || value.length() <= 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }
    
    private boolean isValidLuhn(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }
    
    private int calculateRiskScore(SanitizationAnalysis analysis) {
        int score = 0;
        
        if (analysis.isHasMetadata()) score += 10;
        if (analysis.isHasXmpMetadata()) score += 10;
        if (analysis.isHasJavaScript()) score += 30;
        if (analysis.getAttachmentCount() > 0) score += 15;
        if (analysis.getAnnotationCount() > 0) score += 5;
        if (analysis.isHasPotentialPII()) score += 25;
        if (analysis.getThumbnailCount() > 0) score += 5;
        
        return Math.min(100, score);
    }
    
    private String getRiskLevel(int score) {
        if (score < 20) return "LOW";
        if (score < 50) return "MEDIUM";
        if (score < 75) return "HIGH";
        return "CRITICAL";
    }
    
    private List<String> generateRecommendations(SanitizationAnalysis analysis) {
        List<String> recommendations = new ArrayList<>();
        
        if (analysis.isHasMetadata()) {
            recommendations.add("Remove document metadata (author, title, creation dates) before sharing");
        }
        if (analysis.isHasXmpMetadata()) {
            recommendations.add("Remove XMP metadata stream which may contain detailed editing history");
        }
        if (analysis.isHasJavaScript()) {
            recommendations.add("CRITICAL: Remove JavaScript - can execute malicious code");
        }
        if (analysis.getAttachmentCount() > 0) {
            recommendations.add("Remove embedded files/attachments - may contain sensitive data");
        }
        if (analysis.isHasPotentialPII()) {
            recommendations.add("Review and redact potential personal information found in document");
        }
        if (analysis.getThumbnailCount() > 0) {
            recommendations.add("Remove thumbnail images - may reveal content of deleted pages");
        }
        
        return recommendations;
    }
    
    // ==================== RESULT CLASSES ====================
    
    public static class SanitizationOptions {
        private boolean removeMetadata = true;
        private boolean removeXmpMetadata = true;
        private boolean removeJavaScript = true;
        private boolean removeAttachments = true;
        private boolean removeAnnotations = false;
        private boolean removeFormData = false;
        private boolean removeLinks = false;
        private boolean removeThumbnails = true;
        private boolean removeBookmarks = false;
        private boolean removeHiddenLayers = true;
        private boolean removePrivateData = true;
        private boolean clearHistory = true;
        private boolean scanForPII = true;
        
        // Builder-style setters
        public SanitizationOptions removeMetadata(boolean v) { this.removeMetadata = v; return this; }
        public SanitizationOptions removeXmpMetadata(boolean v) { this.removeXmpMetadata = v; return this; }
        public SanitizationOptions removeJavaScript(boolean v) { this.removeJavaScript = v; return this; }
        public SanitizationOptions removeAttachments(boolean v) { this.removeAttachments = v; return this; }
        public SanitizationOptions removeAnnotations(boolean v) { this.removeAnnotations = v; return this; }
        public SanitizationOptions removeFormData(boolean v) { this.removeFormData = v; return this; }
        public SanitizationOptions removeLinks(boolean v) { this.removeLinks = v; return this; }
        public SanitizationOptions removeThumbnails(boolean v) { this.removeThumbnails = v; return this; }
        public SanitizationOptions removeBookmarks(boolean v) { this.removeBookmarks = v; return this; }
        public SanitizationOptions removeHiddenLayers(boolean v) { this.removeHiddenLayers = v; return this; }
        public SanitizationOptions removePrivateData(boolean v) { this.removePrivateData = v; return this; }
        public SanitizationOptions clearHistory(boolean v) { this.clearHistory = v; return this; }
        public SanitizationOptions scanForPII(boolean v) { this.scanForPII = v; return this; }
        
        // Getters
        public boolean isRemoveMetadata() { return removeMetadata; }
        public boolean isRemoveXmpMetadata() { return removeXmpMetadata; }
        public boolean isRemoveJavaScript() { return removeJavaScript; }
        public boolean isRemoveAttachments() { return removeAttachments; }
        public boolean isRemoveAnnotations() { return removeAnnotations; }
        public boolean isRemoveFormData() { return removeFormData; }
        public boolean isRemoveLinks() { return removeLinks; }
        public boolean isRemoveThumbnails() { return removeThumbnails; }
        public boolean isRemoveBookmarks() { return removeBookmarks; }
        public boolean isRemoveHiddenLayers() { return removeHiddenLayers; }
        public boolean isRemovePrivateData() { return removePrivateData; }
        public boolean isClearHistory() { return clearHistory; }
        public boolean isScanForPII() { return scanForPII; }
        
        public static SanitizationOptions full() {
            return new SanitizationOptions()
                .removeAnnotations(true)
                .removeFormData(true)
                .removeLinks(true)
                .removeBookmarks(true);
        }
        
        @Override
        public String toString() {
            return "SanitizationOptions{metadata=" + removeMetadata + ", xmp=" + removeXmpMetadata + 
                   ", js=" + removeJavaScript + ", attachments=" + removeAttachments + "}";
        }
    }
    
    public static class SanitizationResult {
        private String inputFile;
        private String outputFile;
        private boolean success;
        private String error;
        private long processingTimeMs;
        private long inputSize;
        private long outputSize;
        private Map<String, Integer> removedItems = new LinkedHashMap<>();
        private List<PIIFinding> piiFindings = new ArrayList<>();
        
        public void addRemovedItem(String type, int count) {
            if (count > 0) removedItems.put(type, count);
        }
        
        public int getTotalItemsRemoved() {
            return removedItems.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        // Getters and Setters
        public String getInputFile() { return inputFile; }
        public void setInputFile(String inputFile) { this.inputFile = inputFile; }
        public String getOutputFile() { return outputFile; }
        public void setOutputFile(String outputFile) { this.outputFile = outputFile; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
        public long getInputSize() { return inputSize; }
        public void setInputSize(long inputSize) { this.inputSize = inputSize; }
        public long getOutputSize() { return outputSize; }
        public void setOutputSize(long outputSize) { this.outputSize = outputSize; }
        public Map<String, Integer> getRemovedItems() { return removedItems; }
        public List<PIIFinding> getPiiFindings() { return piiFindings; }
        public void setPiiFindings(List<PIIFinding> piiFindings) { this.piiFindings = piiFindings; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("success", success);
            map.put("inputFile", inputFile);
            map.put("outputFile", outputFile);
            map.put("inputSize", inputSize);
            map.put("outputSize", outputSize);
            map.put("sizeSaved", inputSize - outputSize);
            map.put("processingTimeMs", processingTimeMs);
            map.put("totalItemsRemoved", getTotalItemsRemoved());
            map.put("removedItems", removedItems);
            map.put("piiDetected", piiFindings.size());
            if (error != null) map.put("error", error);
            return map;
        }
    }
    
    public static class SanitizationAnalysis {
        private String filePath;
        private boolean hasMetadata;
        private Map<String, String> documentMetadata;
        private boolean hasXmpMetadata;
        private boolean hasJavaScript;
        private int attachmentCount;
        private int annotationCount;
        private int formFieldCount;
        private boolean hasBookmarks;
        private int thumbnailCount;
        private int hiddenLayerCount;
        private boolean hasPotentialPII;
        private List<PIIFinding> piiFindings;
        private int riskScore;
        private String riskLevel;
        private List<String> recommendations;
        private String error;
        
        // Getters and Setters
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public boolean isHasMetadata() { return hasMetadata; }
        public void setHasMetadata(boolean hasMetadata) { this.hasMetadata = hasMetadata; }
        public Map<String, String> getDocumentMetadata() { return documentMetadata; }
        public void setDocumentMetadata(Map<String, String> documentMetadata) { this.documentMetadata = documentMetadata; }
        public boolean isHasXmpMetadata() { return hasXmpMetadata; }
        public void setHasXmpMetadata(boolean hasXmpMetadata) { this.hasXmpMetadata = hasXmpMetadata; }
        public boolean isHasJavaScript() { return hasJavaScript; }
        public void setHasJavaScript(boolean hasJavaScript) { this.hasJavaScript = hasJavaScript; }
        public int getAttachmentCount() { return attachmentCount; }
        public void setAttachmentCount(int attachmentCount) { this.attachmentCount = attachmentCount; }
        public int getAnnotationCount() { return annotationCount; }
        public void setAnnotationCount(int annotationCount) { this.annotationCount = annotationCount; }
        public int getFormFieldCount() { return formFieldCount; }
        public void setFormFieldCount(int formFieldCount) { this.formFieldCount = formFieldCount; }
        public boolean isHasBookmarks() { return hasBookmarks; }
        public void setHasBookmarks(boolean hasBookmarks) { this.hasBookmarks = hasBookmarks; }
        public int getThumbnailCount() { return thumbnailCount; }
        public void setThumbnailCount(int thumbnailCount) { this.thumbnailCount = thumbnailCount; }
        public int getHiddenLayerCount() { return hiddenLayerCount; }
        public void setHiddenLayerCount(int hiddenLayerCount) { this.hiddenLayerCount = hiddenLayerCount; }
        public boolean isHasPotentialPII() { return hasPotentialPII; }
        public void setHasPotentialPII(boolean hasPotentialPII) { this.hasPotentialPII = hasPotentialPII; }
        public List<PIIFinding> getPiiFindings() { return piiFindings; }
        public void setPiiFindings(List<PIIFinding> piiFindings) { this.piiFindings = piiFindings; }
        public int getRiskScore() { return riskScore; }
        public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
    }
    
    public static class PIIFinding {
        private final String type;
        private final String value;
        private final String location;
        
        public PIIFinding(String type, String value, String location) {
            this.type = type;
            this.value = value;
            this.location = location;
        }
        
        public String getType() { return type; }
        public String getValue() { return value; }
        public String getLocation() { return location; }
        
        public Map<String, String> toMap() {
            return Map.of("type", type, "value", value, "location", location);
        }
    }
}
