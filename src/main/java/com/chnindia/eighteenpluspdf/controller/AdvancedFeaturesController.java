package com.chnindia.eighteenpluspdf.controller;

import com.chnindia.eighteenpluspdf.dto.request.*;
import com.chnindia.eighteenpluspdf.dto.response.*;
import com.chnindia.eighteenpluspdf.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Controller for advanced PDF features.
 * Provides endpoints for forms, annotations, AI analysis, bookmarks, webhooks, and validation.
 */
@RestController
@RequestMapping("/pdf/advanced")
public class AdvancedFeaturesController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdvancedFeaturesController.class);
    
    @Value("${pdf.upload.dir:temp}")
    private String uploadDir;
    
    @Value("${pdf.output.dir:temp}")
    private String outputDir;
    
    @Autowired
    private FormService formService;
    
    @Autowired
    private AnnotationService annotationService;
    
    @Autowired
    private AIAnalysisService aiAnalysisService;
    
    @Autowired
    private BookmarkService bookmarkService;
    
    @Autowired
    private WebhookService webhookService;
    
    @Autowired
    private PDFValidationService validationService;
    
    // ==================== Form Management ====================
    
    /**
     * Fill form fields in a PDF.
     */
    @PostMapping(value = "/forms/fill", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> fillForm(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") FormFillRequest request) {
        
        logger.info("Filling form fields in PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            
            Map<String, Object> result = formService.fillForm(inputFile, request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.putAll(result);
            response.put("fieldsProcessed", request.getFormData() != null ? request.getFormData().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error filling form", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Extract form data from a PDF.
     */
    @PostMapping(value = "/forms/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FormDataResponse> extractFormData(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "JSON") String format) {
        
        logger.info("Extracting form data from PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            FormExportRequest exportRequest = new FormExportRequest();
            try {
                exportRequest.setFormat(FormExportRequest.ExportFormat.valueOf(format));
            } catch (IllegalArgumentException e) {
                exportRequest.setFormat(FormExportRequest.ExportFormat.JSON);
            }
            FormDataResponse response = formService.extractFormData(inputFile, exportRequest);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error extracting form data", e);
            FormDataResponse errorResponse = new FormDataResponse();
            errorResponse.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Create a new PDF form.
     */
    @PostMapping(value = "/forms/create", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> createForm(@RequestBody FormCreateRequest request) {
        logger.info("Creating new PDF form");
        
        try {
            Map<String, Object> result = formService.createForm(request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.putAll(result);
            response.put("fieldCount", request.getFields() != null ? request.getFields().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error creating form", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Detect form fields in a PDF.
     */
    @PostMapping(value = "/forms/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> detectFormFields(@RequestPart("file") MultipartFile file) {
        logger.info("Detecting form fields in PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            Map<String, Object> result = formService.detectFormFields(inputFile);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.putAll(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error detecting form fields", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // ==================== Annotations ====================
    
    /**
     * Add annotations to a PDF.
     */
    @PostMapping(value = "/annotations/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addAnnotations(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") AnnotationRequest request) {
        
        logger.info("Adding annotations to PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            
            Map<String, Object> result = annotationService.addAnnotations(inputFile, request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.putAll(result);
            response.put("annotationsAdded", request.getAnnotations() != null ? request.getAnnotations().size() : 0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error adding annotations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * List annotations in a PDF.
     */
    @PostMapping(value = "/annotations/list", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> listAnnotations(@RequestPart("file") MultipartFile file) {
        logger.info("Listing annotations in PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            Map<String, Object> result = annotationService.listAnnotations(inputFile);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.putAll(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error listing annotations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Remove annotations from a PDF.
     */
    @PostMapping(value = "/annotations/remove", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> removeAnnotations(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") AnnotationRequest request) {
        
        logger.info("Removing annotations from PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            Path outputFile = generateOutputPath("cleaned", ".pdf");
            
            List<String> types = request.getTypesToRemove();
            List<Integer> pages = request.getPagesToRemove();
            Map<String, Object> result = annotationService.removeAnnotations(inputFile, types, pages, outputFile.getFileName().toString());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.putAll(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error removing annotations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Flatten annotations (burn into page content).
     */
    @PostMapping(value = "/annotations/flatten", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> flattenAnnotations(@RequestPart("file") MultipartFile file) {
        logger.info("Flattening annotations in PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            Path outputFile = generateOutputPath("flattened", ".pdf");
            
            annotationService.flattenAnnotations(inputFile, outputFile.getFileName().toString());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("outputFile", outputFile.getFileName().toString());
            response.put("downloadUrl", "/api/download/" + outputFile.getFileName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error flattening annotations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // ==================== AI Analysis ====================
    
    /**
     * Perform AI analysis on a PDF document.
     */
    @PostMapping(value = "/ai/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AIAnalysisResponse> analyzeDocument(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") AIAnalysisRequest request) {
        
        logger.info("Performing AI analysis: {}", request.getAnalysisType());
        
        try {
            Path inputFile = saveUploadedFile(file);
            AIAnalysisResponse response = aiAnalysisService.analyzeDocument(inputFile, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in AI analysis", e);
            AIAnalysisResponse errorResponse = new AIAnalysisResponse();
            errorResponse.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Search document content.
     */
    @PostMapping(value = "/ai/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AIAnalysisResponse> searchDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK) {
        
        logger.info("Searching document for: {}", query);
        
        try {
            Path inputFile = saveUploadedFile(file);
            
            AIAnalysisRequest request = new AIAnalysisRequest();
            request.setAnalysisType(AIAnalysisRequest.AnalysisType.SEMANTIC_SEARCH);
            request.setQuery(query);
            
            AIAnalysisRequest.SearchOptions searchOptions = new AIAnalysisRequest.SearchOptions();
            searchOptions.setTopK(topK);
            request.setSearchOptions(searchOptions);
            
            AIAnalysisResponse response = aiAnalysisService.analyzeDocument(inputFile, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error searching document", e);
            AIAnalysisResponse errorResponse = new AIAnalysisResponse();
            errorResponse.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Extract entities from document.
     */
    @PostMapping(value = "/ai/entities", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AIAnalysisResponse> extractEntities(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) List<String> entityTypes) {
        
        logger.info("Extracting entities from document");
        
        try {
            Path inputFile = saveUploadedFile(file);
            
            AIAnalysisRequest request = new AIAnalysisRequest();
            request.setAnalysisType(AIAnalysisRequest.AnalysisType.EXTRACT_ENTITIES);
            request.setEntityTypes(entityTypes);
            
            AIAnalysisResponse response = aiAnalysisService.analyzeDocument(inputFile, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error extracting entities", e);
            AIAnalysisResponse errorResponse = new AIAnalysisResponse();
            errorResponse.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Summarize document.
     */
    @PostMapping(value = "/ai/summarize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AIAnalysisResponse> summarizeDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "500") int maxLength) {
        
        logger.info("Summarizing document");
        
        try {
            Path inputFile = saveUploadedFile(file);
            
            AIAnalysisRequest request = new AIAnalysisRequest();
            request.setAnalysisType(AIAnalysisRequest.AnalysisType.SUMMARIZE);
            
            AIAnalysisRequest.SummaryOptions options = new AIAnalysisRequest.SummaryOptions();
            options.setMaxLength(maxLength);
            options.setIncludeKeywords(true);
            request.setSummaryOptions(options);
            
            AIAnalysisResponse response = aiAnalysisService.analyzeDocument(inputFile, request);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error summarizing document", e);
            AIAnalysisResponse errorResponse = new AIAnalysisResponse();
            errorResponse.setError(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ==================== Bookmarks ====================
    
    /**
     * Add bookmarks to a PDF.
     */
    @PostMapping(value = "/bookmarks/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> addBookmarks(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") BookmarkRequest request) {
        
        logger.info("Adding bookmarks to PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            Path outputFile = generateOutputPath("bookmarked", ".pdf");
            
            bookmarkService.addBookmarks(inputFile, outputFile, request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("outputFile", outputFile.getFileName().toString());
            response.put("bookmarksAdded", request.getBookmarks() != null ? request.getBookmarks().size() : 0);
            response.put("downloadUrl", "/api/download/" + outputFile.getFileName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error adding bookmarks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Remove bookmarks from a PDF.
     */
    @PostMapping(value = "/bookmarks/remove", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> removeBookmarks(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") BookmarkRequest request) {
        
        logger.info("Removing bookmarks from PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            Path outputFile = generateOutputPath("nobookmarks", ".pdf");
            
            bookmarkService.removeBookmarks(inputFile, outputFile, request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("outputFile", outputFile.getFileName().toString());
            response.put("downloadUrl", "/api/download/" + outputFile.getFileName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error removing bookmarks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Extract bookmarks from a PDF.
     */
    @PostMapping(value = "/bookmarks/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> extractBookmarks(@RequestPart("file") MultipartFile file) {
        logger.info("Extracting bookmarks from PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            List<Map<String, Object>> bookmarks = bookmarkService.extractBookmarks(inputFile);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("bookmarkCount", bookmarks.size());
            response.put("bookmarks", bookmarks);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error extracting bookmarks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Auto-generate bookmarks from document structure.
     */
    @PostMapping(value = "/bookmarks/auto-generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> autoGenerateBookmarks(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "request", required = false) BookmarkRequest request) {
        
        logger.info("Auto-generating bookmarks for PDF");
        
        try {
            Path inputFile = saveUploadedFile(file);
            Path outputFile = generateOutputPath("auto_bookmarked", ".pdf");
            
            bookmarkService.autoGenerateBookmarks(inputFile, outputFile, request);
            
            int bookmarkCount = bookmarkService.getBookmarkCount(outputFile);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("outputFile", outputFile.getFileName().toString());
            response.put("bookmarksGenerated", bookmarkCount);
            response.put("downloadUrl", "/api/download/" + outputFile.getFileName());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error auto-generating bookmarks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // ==================== Validation ====================
    
    /**
     * Validate a PDF document.
     */
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ValidationResponse> validatePDF(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "true") boolean checkStructure,
            @RequestParam(defaultValue = "true") boolean checkSearchable,
            @RequestParam(defaultValue = "false") boolean checkPdfA) {
        
        logger.info("Validating PDF document");
        
        try {
            Path inputFile = saveUploadedFile(file);
            
            PDFValidationService.ValidationOptions options = new PDFValidationService.ValidationOptions();
            options.setCheckSearchability(checkSearchable);
            options.setCheckPDFA(checkPdfA);
            
            ValidationResponse response = validationService.validatePDF(inputFile, options);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating PDF", e);
            ValidationResponse errorResponse = new ValidationResponse();
            errorResponse.setValid(false);
            errorResponse.setIssues(List.of(
                createIssue("ERROR", "VALIDATION_ERROR", e.getMessage(), 0)
            ));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ==================== Webhooks ====================
    
    /**
     * Register a webhook.
     */
    @PostMapping("/webhooks/register")
    public ResponseEntity<Map<String, Object>> registerWebhook(@RequestBody WebhookRequest request) {
        logger.info("Registering webhook: {}", request.getUrl());
        
        try {
            WebhookResponse webhookResponse = webhookService.registerWebhook(request);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("webhookId", webhookResponse.getWebhookId());
            response.put("events", request.getEvents());
            response.put("url", request.getUrl());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error registering webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * Unregister a webhook.
     */
    @DeleteMapping("/webhooks/{webhookId}")
    public ResponseEntity<Map<String, Object>> unregisterWebhook(@PathVariable String webhookId) {
        logger.info("Unregistering webhook: {}", webhookId);
        
        try {
            webhookService.unregisterWebhook(webhookId);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("webhookId", webhookId);
            response.put("message", "Webhook unregistered successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error unregistering webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    /**
     * List registered webhooks.
     */
    @GetMapping("/webhooks")
    public ResponseEntity<Map<String, Object>> listWebhooks() {
        logger.info("Listing webhooks");
        
        try {
            List<WebhookResponse> webhooks = webhookService.listWebhooks();
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("count", webhooks.size());
            response.put("webhooks", webhooks);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error listing webhooks", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    
    // ==================== Utility Methods ====================
    
    private Path saveUploadedFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath);
        
        return filePath;
    }
    
    private Path generateOutputPath(String prefix, String extension) throws IOException {
        Path outputPath = Paths.get(outputDir);
        if (!Files.exists(outputPath)) {
            Files.createDirectories(outputPath);
        }
        
        String filename = prefix + "_" + UUID.randomUUID() + extension;
        return outputPath.resolve(filename);
    }
    
    private ValidationResponse.ValidationIssue createIssue(String severity, String code, String message, int page) {
        ValidationResponse.ValidationIssue issue = new ValidationResponse.ValidationIssue();
        issue.setSeverity(ValidationResponse.ValidationIssue.IssueSeverity.valueOf(severity));
        issue.setCode(code);
        issue.setMessage(message);
        issue.setPageNumber(page);
        return issue;
    }
}
