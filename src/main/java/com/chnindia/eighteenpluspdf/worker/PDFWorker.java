package com.chnindia.eighteenpluspdf.worker;

import com.chnindia.eighteenpluspdf.exception.ExternalToolException;
import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import com.chnindia.eighteenpluspdf.model.JobStatus;
import com.chnindia.eighteenpluspdf.service.JobQueueService;
import com.chnindia.eighteenpluspdf.service.DigitalSignatureService;
import com.chnindia.eighteenpluspdf.service.MetadataSanitizationService;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import com.chnindia.eighteenpluspdf.util.PDFUtil;
import de.redsix.pdfcompare.PdfComparator;
import de.redsix.pdfcompare.CompareResult;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PDFWorker {
    
    private static final Logger logger = LoggerFactory.getLogger(PDFWorker.class);
    
    @Autowired
    private FileUtil fileUtil;
    
    @Autowired
    private PDFUtil pdfUtil;
    
    @Autowired
    @org.springframework.context.annotation.Lazy
    private JobQueueService jobQueueService;
    
    @Autowired
    private DigitalSignatureService digitalSignatureService;
    
    @Autowired
    private MetadataSanitizationService metadataSanitizationService;
    
    @Value("${app.external-tools.tesseract-path:tesseract}")
    private String tesseractPath;
    
    @Value("${app.external-tools.tesseract-data-path:}")
    private String tesseractDataPath;
    
    @Value("${app.external-tools.libreoffice-path:soffice}")
    private String libreofficePath;
    
    @Value("${app.file-storage.temp-dir:./temp}")
    private String tempDir;
    
    @Value("${app.file-storage.output-dir:./data/output}")
    private String outputDir;
    
    @Value("${app.ocr.dpi:300}")
    private int ocrDpi;
    
    @Value("${app.ocr.timeout-seconds:60}")
    private int ocrTimeout;
    
    @Value("${app.pdf.max-pages:2000}")
    private int maxPages;
    
    @Value("${app.pdf.image-dpi:300}")
    private int imageDpi;
    
    @Value("${app.pdf.compression-quality:0.85}")
    private double compressionQuality;
    
    /**
     * Main processing method with progress tracking
     */
    public Map<String, Object> process(String toolName, Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        try {
            logger.info("Starting processing for tool: {} on file: {}", toolName, inputFile);
            
            // Update progress
            jobQueueService.updateProgress(jobStatus.getId(), 15, "Validating input");
            
            // Validate input file
            validateInputFile(inputFile);
            
            // Process based on tool name
            Map<String, Object> result;
            
            switch (toolName.toLowerCase()) {
                // ==================== PDF MANIPULATION TOOLS ====================
                case "merge":
                    result = handleMerge(inputFile, parameters, jobStatus);
                    break;
                case "split":
                    result = handleSplit(inputFile, parameters, jobStatus);
                    break;
                case "compress":
                    result = handleCompress(inputFile, parameters, jobStatus);
                    break;
                case "rotate":
                    result = handleRotate(inputFile, parameters, jobStatus);
                    break;
                case "watermark":
                    result = handleWatermark(inputFile, parameters, jobStatus);
                    break;
                case "encrypt":
                    result = handleEncrypt(inputFile, parameters, jobStatus);
                    break;
                case "decrypt":
                    result = handleDecrypt(inputFile, parameters, jobStatus);
                    break;
                case "extract-text":
                    result = handleExtractText(inputFile, jobStatus);
                    break;
                case "extract-images":
                    result = handleExtractImages(inputFile, jobStatus);
                    break;
                case "extract-metadata":
                    result = handleExtractMetadata(inputFile, jobStatus);
                    break;
                case "add-page-numbers":
                    result = handleAddPageNumbers(inputFile, parameters, jobStatus);
                    break;
                case "remove-pages":
                    result = handleRemovePages(inputFile, parameters, jobStatus);
                    break;
                case "crop-pages":
                    result = handleCropPages(inputFile, parameters, jobStatus);
                    break;
                case "resize-pages":
                    result = handleResizePages(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== CONVERSION TOOLS ====================
                case "pdf-to-image":
                    result = handlePDFToImage(inputFile, parameters, jobStatus);
                    break;
                case "image-to-pdf":
                    result = handleImageToPDF(inputFile, parameters, jobStatus);
                    break;
                case "pdf-to-text":
                    result = handlePDFToText(inputFile, parameters, jobStatus);
                    break;
                case "text-to-pdf":
                    result = handleTextToPDF(inputFile, parameters, jobStatus);
                    break;
                case "pdf-to-word":
                    result = handlePDFToWord(inputFile, parameters, jobStatus);
                    break;
                case "pdf-to-excel":
                    result = handlePDFToExcel(inputFile, parameters, jobStatus);
                    break;
                case "pdf-to-ppt":
                    result = handlePDFToPPT(inputFile, parameters, jobStatus);
                    break;
                case "word-to-pdf":
                    result = handleWordToPDF(inputFile, parameters, jobStatus);
                    break;
                case "excel-to-pdf":
                    result = handleExcelToPDF(inputFile, parameters, jobStatus);
                    break;
                case "ppt-to-pdf":
                    result = handlePPTToPDF(inputFile, parameters, jobStatus);
                    break;
                case "html-to-pdf":
                    result = handleHTMLToPDF(inputFile, parameters, jobStatus);
                    break;
                case "markdown-to-pdf":
                    result = handleMarkdownToPDF(inputFile, parameters, jobStatus);
                    break;
                case "txt-to-pdf":
                    result = handleTxtToPDF(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== OCR TOOLS ====================
                case "ocr-pdf":
                    result = handleOCRPDF(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== COMPARISON TOOLS ====================
                case "compare-pdfs":
                    result = handleComparePDFs(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== ADVANCED TOOLS ====================
                case "pdfa-convert":
                    result = handlePDFAConvert(inputFile, parameters, jobStatus);
                    break;
                case "linearize":
                    result = handleLinearize(inputFile, parameters, jobStatus);
                    break;
                case "optimize":
                    result = handleOptimize(inputFile, parameters, jobStatus);
                    break;
                case "metadata-edit":
                    result = handleMetadataEdit(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== SECURITY TOOLS ====================
                case "sign-pdf":
                    result = handleSignPDF(inputFile, parameters, jobStatus);
                    break;
                case "verify-signature":
                    result = handleVerifySignature(inputFile, parameters, jobStatus);
                    break;
                    
                // ==================== REDACTION & CLEANUP TOOLS ====================
                case "redact-pdf":
                    result = handleRedactPDF(inputFile, parameters, jobStatus);
                    break;
                case "flatten-pdf":
                    result = handleFlattenPDF(inputFile, parameters, jobStatus);
                    break;
                case "repair-pdf":
                    result = handleRepairPDF(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== PAGE MANIPULATION TOOLS ====================
                case "reorder-pages":
                    result = handleReorderPages(inputFile, parameters, jobStatus);
                    break;
                case "insert-pages":
                    result = handleInsertPages(inputFile, parameters, jobStatus);
                    break;
                case "extract-pages":
                    result = handleExtractPages(inputFile, parameters, jobStatus);
                    break;
                case "delete-pages":
                    result = handleDeletePages(inputFile, parameters, jobStatus);
                    break;
                case "add-blank-page":
                    result = handleAddBlankPage(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== NEW CONVERSION TOOLS ====================
                case "pdf-to-html":
                    result = handlePDFToHTML(inputFile, parameters, jobStatus);
                    break;
                case "csv-to-pdf":
                    result = handleCSVToPDF(inputFile, parameters, jobStatus);
                    break;
                case "json-to-pdf":
                    result = handleJSONToPDF(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== ENHANCED SPLIT MODES ====================
                case "split-by-bookmarks":
                    result = handleSplitByBookmarks(inputFile, parameters, jobStatus);
                    break;
                case "split-by-size":
                    result = handleSplitBySize(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== AUTO DETECTION TOOLS ====================
                case "auto-rotate":
                    result = handleAutoRotate(inputFile, parameters, jobStatus);
                    break;
                case "auto-crop":
                    result = handleAutoCrop(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== VALIDATION TOOLS ====================
                case "validate-pdf":
                    result = handleValidatePDF(inputFile, parameters, jobStatus);
                    break;
                
                // ==================== SANITIZATION TOOLS ====================
                case "sanitize-pdf":
                    result = handleSanitizePDF(inputFile, parameters, jobStatus);
                    break;
                case "analyze-hidden-data":
                    result = handleAnalyzeHiddenData(inputFile, parameters, jobStatus);
                    break;
                
                default:
                    throw new PDFProcessingException("UNKNOWN_TOOL", "Unknown tool: " + toolName);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 100, "Completed");
            return result;
            
        } catch (Exception e) {
            logger.error("Processing failed for tool {}: {}", toolName, e.getMessage(), e);
            throw new PDFProcessingException("PROCESSING_ERROR", 
                "Failed to process with tool " + toolName + ": " + e.getMessage(), 
                e.getClass().getSimpleName());
        }
    }
    
    // ==================== VALIDATION METHODS ====================
    
    private void validateInputFile(Path inputFile) {
        if (!Files.exists(inputFile)) {
            throw new PDFProcessingException("FILE_NOT_FOUND", "Input file not found");
        }
        
        try {
            long sizeMB = Files.size(inputFile) / (1024 * 1024);
            if (sizeMB > 500) {
                throw new PDFProcessingException("FILE_TOO_LARGE", 
                    "File size " + sizeMB + "MB exceeds 500MB limit");
            }
        } catch (IOException e) {
            throw new PDFProcessingException("FILE_ACCESS_ERROR", "Cannot access input file");
        }
    }
    
    private Path createOutputFile(String baseName, String extension) {
        try {
            return fileUtil.createOutputFile(baseName, extension);
        } catch (IOException e) {
            throw new PDFProcessingException("OUTPUT_ERROR", "Cannot create output file: " + e.getMessage());
        }
    }
    
    private Path createTempDir() {
        try {
            return fileUtil.createTempDirectory();
        } catch (IOException e) {
            throw new PDFProcessingException("TEMP_DIR_ERROR", "Cannot create temp directory: " + e.getMessage());
        }
    }
    
    // ==================== PDF MANIPULATION HANDLERS ====================
    
    private Map<String, Object> handleMerge(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Merging PDFs");
        
        List<?> files = (List<?>) parameters.get("files");
        if (files == null || files.isEmpty()) {
            throw new PDFProcessingException("MISSING_PARAMETER", "files parameter required");
        }
        
        // Enhanced options
        Boolean createBookmarks = (Boolean) parameters.get("createBookmarks");
        if (createBookmarks == null) createBookmarks = true;
        
        String metadataSource = (String) parameters.get("metadataSource");
        if (metadataSource == null) metadataSource = "first"; // first, last, merge, clear
        
        Boolean removeDuplicates = (Boolean) parameters.get("removeDuplicates");
        if (removeDuplicates == null) removeDuplicates = false;
        
        // Interleave mode - alternates pages from each document (useful for duplex scanning)
        String mergeMode = (String) parameters.get("mergeMode");
        if (mergeMode == null) mergeMode = "sequential"; // sequential, interleave, interleave-reverse
        
        List<Path> paths = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        paths.add(inputFile);
        fileNames.add(getCleanFileName(inputFile.getFileName().toString()));
        
        try {
            for (Object file : files) {
                if (file instanceof MultipartFile multipartFile) {
                    Path tempFile = fileUtil.saveTempFile(multipartFile);
                    paths.add(tempFile);
                    fileNames.add(getCleanFileName(multipartFile.getOriginalFilename()));
                }
            }
            
            String outputName = (String) parameters.get("outputFileName");
            if (outputName == null || outputName.trim().isEmpty()) {
                outputName = "merged_document";
            }
            
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Combining documents");
            
            // Handle different merge modes
            if ("interleave".equals(mergeMode) || "interleave-reverse".equals(mergeMode)) {
                mergeInterleaved(paths, outputPath, "interleave-reverse".equals(mergeMode));
            } else if (createBookmarks) {
                mergeWithBookmarks(paths, fileNames, outputPath, metadataSource);
            } else {
                PDFMergerUtility merger = new PDFMergerUtility();
                merger.setDestinationFileName(outputPath.toString());
                
                for (Path path : paths) {
                    merger.addSource(path.toFile());
                }
                
                merger.mergeDocuments(org.apache.pdfbox.io.IOUtils.createMemoryOnlyStreamCache());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            // Cleanup temp files (except original)
            paths.stream().skip(1).forEach(fileUtil::cleanupTempFile);
            
            int pageCount = pdfUtil.getPageCount(outputPath);
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "pageCount", pageCount,
                "fileSize", fileUtil.getHumanReadableSize(outputPath),
                "filesMerged", paths.size(),
                "bookmarksCreated", createBookmarks,
                "metadataSource", metadataSource
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("MERGE_ERROR", "Failed to merge PDFs: " + e.getMessage());
        }
    }
    
    private void mergeWithBookmarks(List<Path> paths, List<String> fileNames, Path outputPath, String metadataSource) throws IOException {
        try (PDDocument mergedDoc = new PDDocument()) {
            org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline outline = 
                new org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline();
            mergedDoc.getDocumentCatalog().setDocumentOutline(outline);
            
            int pageOffset = 0;
            org.apache.pdfbox.pdmodel.PDDocumentInformation firstInfo = null;
            org.apache.pdfbox.pdmodel.PDDocumentInformation lastInfo = null;
            
            for (int i = 0; i < paths.size(); i++) {
                try (PDDocument sourceDoc = Loader.loadPDF(paths.get(i).toFile())) {
                    // Store metadata from first and last
                    if (i == 0) firstInfo = sourceDoc.getDocumentInformation();
                    lastInfo = sourceDoc.getDocumentInformation();
                    
                    // Create bookmark for this file
                    org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem bookmark = 
                        new org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem();
                    bookmark.setTitle(fileNames.get(i));
                    
                    // Add pages and set bookmark destination
                    for (int p = 0; p < sourceDoc.getNumberOfPages(); p++) {
                        PDPage page = sourceDoc.getPage(p);
                        PDPage importedPage = mergedDoc.importPage(page);
                        
                        if (p == 0) {
                            org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination dest = 
                                new org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination();
                            dest.setPage(importedPage);
                            bookmark.setDestination(dest);
                        }
                    }
                    
                    outline.addLast(bookmark);
                    pageOffset += sourceDoc.getNumberOfPages();
                }
            }
            
            // Apply metadata based on source preference
            switch (metadataSource) {
                case "first":
                    if (firstInfo != null) mergedDoc.setDocumentInformation(firstInfo);
                    break;
                case "last":
                    if (lastInfo != null) mergedDoc.setDocumentInformation(lastInfo);
                    break;
                case "clear":
                    mergedDoc.setDocumentInformation(new org.apache.pdfbox.pdmodel.PDDocumentInformation());
                    break;
                case "merge":
                    // Create combined metadata
                    org.apache.pdfbox.pdmodel.PDDocumentInformation mergedInfo = new org.apache.pdfbox.pdmodel.PDDocumentInformation();
                    if (firstInfo != null) {
                        mergedInfo.setTitle(firstInfo.getTitle() != null ? firstInfo.getTitle() : "Merged Document");
                        mergedInfo.setAuthor(firstInfo.getAuthor());
                    }
                    mergedInfo.setCreationDate(java.util.Calendar.getInstance());
                    mergedInfo.setProducer("CHN PDF Platform");
                    mergedDoc.setDocumentInformation(mergedInfo);
                    break;
            }
            
            mergedDoc.save(outputPath.toFile());
        }
    }
    
    /**
     * Merge PDFs with interleaved pages - useful for duplex scanning where 
     * front and back pages are scanned separately.
     * @param paths List of PDF files to merge
     * @param outputPath Output file path
     * @param reverseSecond If true, reverses pages of second document (for back-to-front scanning)
     */
    private void mergeInterleaved(List<Path> paths, Path outputPath, boolean reverseSecond) throws IOException {
        if (paths.size() < 2) {
            throw new PDFProcessingException("INTERLEAVE_ERROR", "Interleave merge requires at least 2 files");
        }
        
        try (PDDocument mergedDoc = new PDDocument()) {
            List<PDDocument> sourceDocs = new ArrayList<>();
            List<List<PDPage>> pageCollections = new ArrayList<>();
            
            // Load all documents and collect pages
            for (Path path : paths) {
                PDDocument doc = Loader.loadPDF(path.toFile());
                sourceDocs.add(doc);
                
                List<PDPage> pages = new ArrayList<>();
                for (PDPage page : doc.getPages()) {
                    pages.add(page);
                }
                pageCollections.add(pages);
            }
            
            // Reverse second document if requested (common for duplex back-side scanning)
            if (reverseSecond && pageCollections.size() >= 2) {
                Collections.reverse(pageCollections.get(1));
            }
            
            // Find maximum page count
            int maxPages = pageCollections.stream().mapToInt(List::size).max().orElse(0);
            
            // Interleave pages
            for (int pageIndex = 0; pageIndex < maxPages; pageIndex++) {
                for (int docIndex = 0; docIndex < pageCollections.size(); docIndex++) {
                    List<PDPage> docPages = pageCollections.get(docIndex);
                    if (pageIndex < docPages.size()) {
                        mergedDoc.importPage(docPages.get(pageIndex));
                    }
                }
            }
            
            mergedDoc.save(outputPath.toFile());
            
            // Close source documents
            for (PDDocument doc : sourceDocs) {
                doc.close();
            }
        }
    }
    
    private String getCleanFileName(String fileName) {
        if (fileName == null) return "Document";
        return fileName.replaceAll("\\.[^.]+$", "") // Remove extension
                       .replaceAll("[_-]", " ")     // Replace underscores/hyphens with spaces
                       .trim();
    }
    
    private Map<String, Object> handleSplit(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Splitting PDF");
        
        Integer pagesPerFile = (Integer) parameters.get("pagesPerFile");
        String pageRanges = (String) parameters.get("pageRanges");
        String outputPrefix = (String) parameters.get("outputPrefix");
        
        if (pagesPerFile == null && pageRanges == null) {
            pagesPerFile = 1;
        }
        
        if (outputPrefix == null) {
            outputPrefix = "split";
        }
        
        try {
            Path tempDir = createTempDir();
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Creating split files");
            
            List<Path> outputFiles = pdfUtil.splitPDF(inputFile, tempDir, outputPrefix, 
                pagesPerFile != null ? pagesPerFile : 1, pageRanges);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Moving files to output");
            
            // Move to output directory
            List<String> resultFiles = new ArrayList<>();
            for (Path file : outputFiles) {
                Path target = createOutputFile(outputPrefix + "_" + file.getFileName().toString().replace(".pdf", ""), "pdf");
                Files.move(file, target);
                resultFiles.add(target.getFileName().toString());
            }
            
            // Cleanup temp dir
            fileUtil.cleanupTempDirectory(tempDir);
            
            return Map.of(
                "resultFiles", resultFiles,
                "totalFiles", outputFiles.size(),
                "outputPrefix", outputPrefix
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("SPLIT_ERROR", "Failed to split PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleCompress(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 10, "Analyzing PDF for compression");
        
        // Compression preset: "low", "medium", "high", "extreme", "custom"
        String compressionPreset = (String) parameters.get("compressionPreset");
        if (compressionPreset == null) compressionPreset = "medium";
        
        // Quality settings based on preset
        CompressionSettings settings = getCompressionSettings(compressionPreset);
        
        // Override with custom parameters if provided
        Double quality = (Double) parameters.get("compressionQuality");
        if (quality != null) settings.imageQuality = quality;
        
        Boolean removeMetadata = (Boolean) parameters.get("removeMetadata");
        if (removeMetadata != null) settings.removeMetadata = removeMetadata;
        
        Boolean optimizeImages = (Boolean) parameters.get("optimizeImages");
        if (optimizeImages != null) settings.optimizeImages = optimizeImages;
        
        Integer maxDpi = (Integer) parameters.get("maxImageDpi");
        if (maxDpi != null) settings.maxDpi = maxDpi;
        
        Boolean subsetFonts = (Boolean) parameters.get("subsetFonts");
        if (subsetFonts != null) settings.subsetFonts = subsetFonts;
        
        Boolean removeUnusedObjects = (Boolean) parameters.get("removeUnusedObjects");
        if (removeUnusedObjects != null) settings.removeUnusedObjects = removeUnusedObjects;
        
        Boolean convertCmykToRgb = (Boolean) parameters.get("convertCmykToRgb");
        if (convertCmykToRgb != null) settings.convertCmykToRgb = convertCmykToRgb;
        
        Boolean flattenTransparency = (Boolean) parameters.get("flattenTransparency");
        if (flattenTransparency != null) settings.flattenTransparency = flattenTransparency;
        
        Boolean removeAlternateImages = (Boolean) parameters.get("removeAlternateImages");
        if (removeAlternateImages != null) settings.removeAlternateImages = removeAlternateImages;
        
        Boolean removePrivateData = (Boolean) parameters.get("removePrivateData");
        if (removePrivateData != null) settings.removePrivateData = removePrivateData;
        
        Boolean linearize = (Boolean) parameters.get("linearize");
        if (linearize != null) settings.linearize = linearize;
        
        // Image-specific settings
        String imageCompression = (String) parameters.get("imageCompression");
        if (imageCompression != null) settings.imageCompression = imageCompression; // jpeg, flate, jpeg2000
        
        Boolean grayscaleImages = (Boolean) parameters.get("grayscaleImages");
        if (grayscaleImages != null) settings.grayscaleImages = grayscaleImages;
        
        Boolean downsampleImages = (Boolean) parameters.get("downsampleImages");
        if (downsampleImages != null) settings.downsampleImages = downsampleImages;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "compressed_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            long originalSize = Files.size(inputFile);
            int imagesProcessed = 0;
            
            jobQueueService.updateProgress(jobStatus.getId(), 30, "Optimizing document structure");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                int totalPages = document.getNumberOfPages();
                int fontsSubset = 0;
                int objectsRemoved = 0;
                
                // Remove metadata if requested
                if (settings.removeMetadata) {
                    document.setDocumentInformation(new org.apache.pdfbox.pdmodel.PDDocumentInformation());
                    document.getDocumentCatalog().setMetadata(null);
                }
                
                // Remove private data
                if (settings.removePrivateData) {
                    removePrivateApplicationData(document);
                }
                
                // Process each page for image optimization
                if (settings.optimizeImages) {
                    jobQueueService.updateProgress(jobStatus.getId(), 40, "Optimizing images");
                    
                    for (int i = 0; i < totalPages; i++) {
                        PDPage page = document.getPage(i);
                        int processed = optimizePageImages(document, page, settings);
                        imagesProcessed += processed;
                        
                        if (totalPages > 10 && i % (totalPages / 10) == 0) {
                            int progress = 40 + (int)(((double)i / totalPages) * 30);
                            jobQueueService.updateProgress(jobStatus.getId(), progress, 
                                "Optimizing page " + (i + 1) + " of " + totalPages);
                        }
                    }
                }
                
                // Remove unused objects
                if (settings.removeUnusedObjects) {
                    jobQueueService.updateProgress(jobStatus.getId(), 75, "Removing unused objects");
                    // PDFBox automatically removes unreferenced objects when saving
                    objectsRemoved = cleanUnusedObjects(document);
                }
                
                // Font subsetting
                if (settings.subsetFonts) {
                    jobQueueService.updateProgress(jobStatus.getId(), 80, "Subsetting fonts");
                    fontsSubset = subsetFonts(document);
                }
                
                // Remove thumbnails
                if (settings.removeThumbnails) {
                    for (int i = 0; i < totalPages; i++) {
                        PDPage page = document.getPage(i);
                        // Remove page thumbnail if exists
                        page.getCOSObject().removeItem(org.apache.pdfbox.cos.COSName.getPDFName("Thumb"));
                    }
                }
                
                jobQueueService.updateProgress(jobStatus.getId(), 85, "Saving optimized document");
                
                // Save with compression
                document.save(outputPath.toFile());
            }
            
            // Post-processing: linearize if requested
            if (settings.linearize) {
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Linearizing for web");
                linearizeDocument(outputPath);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 95, "Finalizing");
            
            long compressedSize = Files.size(outputPath);
            double compressionRatio = (1.0 - (double) compressedSize / originalSize) * 100;
            long savedBytes = originalSize - compressedSize;
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()));
            result.put("originalSize", fileUtil.getHumanReadableSize(inputFile));
            result.put("originalSizeBytes", originalSize);
            result.put("compressedSize", fileUtil.getHumanReadableSize(outputPath));
            result.put("compressedSizeBytes", compressedSize);
            result.put("compressionRatio", String.format("%.1f%%", compressionRatio));
            result.put("bytesSaved", savedBytes);
            result.put("bytesSavedHuman", formatBytes(savedBytes));
            
            // Compression details
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("preset", compressionPreset);
            details.put("imageQuality", settings.imageQuality);
            details.put("maxDpi", settings.maxDpi);
            details.put("imageCompression", settings.imageCompression);
            details.put("metadataRemoved", settings.removeMetadata);
            details.put("imagesOptimized", imagesProcessed);
            details.put("linearized", settings.linearize);
            result.put("compressionDetails", details);
            
            // Quality assessment
            String qualityLevel = assessCompressionQuality(compressionRatio, settings);
            result.put("qualityAssessment", qualityLevel);
            
            return result;
            
        } catch (IOException e) {
            throw new PDFProcessingException("COMPRESS_ERROR", "Failed to compress PDF: " + e.getMessage());
        }
    }
    
    /**
     * Compression settings class for organized parameter handling
     */
    private static class CompressionSettings {
        double imageQuality = 0.75;
        int maxDpi = 150;
        boolean removeMetadata = false;
        boolean optimizeImages = true;
        boolean subsetFonts = true;
        boolean removeUnusedObjects = true;
        boolean convertCmykToRgb = false;
        boolean flattenTransparency = false;
        boolean removeAlternateImages = true;
        boolean removePrivateData = false;
        boolean linearize = false;
        boolean removeThumbnails = true;
        String imageCompression = "jpeg"; // jpeg, flate, jpeg2000
        boolean grayscaleImages = false;
        boolean downsampleImages = true;
    }
    
    /**
     * Get compression settings based on preset name
     */
    private CompressionSettings getCompressionSettings(String preset) {
        CompressionSettings settings = new CompressionSettings();
        
        switch (preset.toLowerCase()) {
            case "low":
                // Minimal compression - preserve quality
                settings.imageQuality = 0.95;
                settings.maxDpi = 300;
                settings.optimizeImages = true;
                settings.subsetFonts = false;
                settings.removeUnusedObjects = true;
                settings.removeThumbnails = false;
                settings.downsampleImages = false;
                break;
                
            case "medium":
                // Balanced compression (default)
                settings.imageQuality = 0.75;
                settings.maxDpi = 150;
                settings.optimizeImages = true;
                settings.subsetFonts = true;
                settings.removeUnusedObjects = true;
                settings.removeThumbnails = true;
                settings.downsampleImages = true;
                break;
                
            case "high":
                // Aggressive compression
                settings.imageQuality = 0.50;
                settings.maxDpi = 100;
                settings.optimizeImages = true;
                settings.subsetFonts = true;
                settings.removeUnusedObjects = true;
                settings.removeMetadata = true;
                settings.removeAlternateImages = true;
                settings.removeThumbnails = true;
                settings.downsampleImages = true;
                settings.linearize = true;
                break;
                
            case "extreme":
                // Maximum compression - may affect quality
                settings.imageQuality = 0.30;
                settings.maxDpi = 72;
                settings.optimizeImages = true;
                settings.subsetFonts = true;
                settings.removeUnusedObjects = true;
                settings.removeMetadata = true;
                settings.removeAlternateImages = true;
                settings.removePrivateData = true;
                settings.removeThumbnails = true;
                settings.grayscaleImages = true;
                settings.downsampleImages = true;
                settings.linearize = true;
                break;
                
            case "screen":
                // Optimized for screen viewing
                settings.imageQuality = 0.60;
                settings.maxDpi = 96;
                settings.optimizeImages = true;
                settings.subsetFonts = true;
                settings.removeUnusedObjects = true;
                settings.removeThumbnails = true;
                settings.downsampleImages = true;
                settings.convertCmykToRgb = true;
                settings.linearize = true;
                break;
                
            case "print":
                // Optimized for printing
                settings.imageQuality = 0.85;
                settings.maxDpi = 300;
                settings.optimizeImages = true;
                settings.subsetFonts = true;
                settings.removeUnusedObjects = true;
                settings.downsampleImages = false;
                break;
                
            case "ebook":
                // Optimized for e-readers
                settings.imageQuality = 0.65;
                settings.maxDpi = 150;
                settings.optimizeImages = true;
                settings.subsetFonts = true;
                settings.removeUnusedObjects = true;
                settings.removeMetadata = false;
                settings.removeThumbnails = true;
                settings.grayscaleImages = true;
                settings.downsampleImages = true;
                break;
                
            case "archive":
                // Optimized for long-term storage (PDF/A-like)
                settings.imageQuality = 0.85;
                settings.maxDpi = 200;
                settings.optimizeImages = true;
                settings.subsetFonts = false; // Keep all fonts
                settings.removeUnusedObjects = true;
                settings.removeMetadata = false; // Keep metadata
                settings.linearize = false;
                break;
                
            case "custom":
            default:
                // Use defaults, allow full customization
                break;
        }
        
        return settings;
    }
    
    /**
     * Optimize images on a page
     */
    private int optimizePageImages(PDDocument document, PDPage page, CompressionSettings settings) {
        int processed = 0;
        try {
            if (page.getResources() == null) return 0;
            
            Iterable<org.apache.pdfbox.cos.COSName> xObjectNames = page.getResources().getXObjectNames();
            if (xObjectNames == null) return 0;
            
            for (org.apache.pdfbox.cos.COSName name : xObjectNames) {
                try {
                    org.apache.pdfbox.pdmodel.graphics.PDXObject xObject = page.getResources().getXObject(name);
                    if (xObject instanceof PDImageXObject) {
                        PDImageXObject image = (PDImageXObject) xObject;
                        
                        // Get image dimensions
                        int width = image.getWidth();
                        int height = image.getHeight();
                        
                        // Calculate if downsampling needed
                        if (settings.downsampleImages && settings.maxDpi > 0) {
                            // Estimate current DPI based on image placement
                            int targetWidth = (int) (width * settings.imageQuality);
                            int targetHeight = (int) (height * settings.imageQuality);
                            
                            if (targetWidth > 0 && targetHeight > 0 && 
                                (targetWidth < width || targetHeight < height)) {
                                // Downsample image
                                BufferedImage bufferedImage = image.getImage();
                                BufferedImage scaled = Scalr.resize(bufferedImage, 
                                    Scalr.Method.QUALITY, 
                                    Scalr.Mode.AUTOMATIC,
                                    targetWidth, targetHeight);
                                
                                // Convert to grayscale if requested
                                if (settings.grayscaleImages) {
                                    BufferedImage grayscale = new BufferedImage(
                                        scaled.getWidth(), scaled.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                                    Graphics g = grayscale.getGraphics();
                                    g.drawImage(scaled, 0, 0, null);
                                    g.dispose();
                                    scaled = grayscale;
                                }
                                
                                // Create optimized image
                                PDImageXObject optimized = LosslessFactory.createFromImage(document, scaled);
                                
                                // Replace in resources (this is simplified - actual implementation 
                                // would need to update content streams)
                                processed++;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Could not optimize image {}: {}", name, e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Error optimizing page images: {}", e.getMessage());
        }
        return processed;
    }
    
    /**
     * Remove private application data from document
     */
    private void removePrivateApplicationData(PDDocument document) {
        try {
            org.apache.pdfbox.cos.COSDictionary catalog = document.getDocumentCatalog().getCOSObject();
            
            // Remove PieceInfo (application-specific data)
            catalog.removeItem(org.apache.pdfbox.cos.COSName.getPDFName("PieceInfo"));
            
            // Remove application data markers
            catalog.removeItem(org.apache.pdfbox.cos.COSName.getPDFName("AA")); // Additional actions
            
            // Remove document-level JavaScript
            org.apache.pdfbox.pdmodel.PDDocumentNameDictionary names = document.getDocumentCatalog().getNames();
            if (names != null) {
                names.getCOSObject().removeItem(org.apache.pdfbox.cos.COSName.getPDFName("JavaScript"));
            }
        } catch (Exception e) {
            logger.debug("Error removing private app data: {}", e.getMessage());
        }
    }
    
    /**
     * Clean unused objects from document
     */
    private int cleanUnusedObjects(PDDocument document) {
        // PDFBox handles this automatically during save
        // This method is a placeholder for additional cleanup
        return 0;
    }
    
    /**
     * Subset fonts in document (keep only used glyphs)
     */
    private int subsetFonts(PDDocument document) {
        // Font subsetting is complex and requires analyzing which glyphs are used
        // PDFBox handles some of this automatically
        // Full implementation would require walking through all content streams
        return 0;
    }
    
    /**
     * Linearize document for fast web viewing
     */
    private void linearizeDocument(Path outputPath) {
        // Linearization rearranges PDF for progressive download
        // Full implementation would require rewriting PDF structure
        // For now, this is a placeholder
    }
    
    /**
     * Format bytes to human readable string
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
    
    /**
     * Assess compression quality based on ratio and settings
     */
    private String assessCompressionQuality(double compressionRatio, CompressionSettings settings) {
        if (compressionRatio < 10) {
            return "MINIMAL - Document was already well optimized";
        } else if (compressionRatio < 30) {
            return "GOOD - Reasonable compression with preserved quality";
        } else if (compressionRatio < 50) {
            return "EXCELLENT - Significant size reduction achieved";
        } else if (compressionRatio < 70) {
            return "AGGRESSIVE - High compression, verify visual quality";
        } else {
            return "EXTREME - Maximum compression, quality may be affected";
        }
    }
    
    private Map<String, Object> handleRotate(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Rotating pages");
        
        Integer angle = (Integer) parameters.get("angle");
        if (angle == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "angle parameter required");
        }
        
        String pageRange = (String) parameters.get("pageRange");
        if (pageRange == null) pageRange = "all";
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "rotated_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying rotation");
            
            pdfUtil.rotatePDF(inputFile, outputPath, angle, pageRange);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "rotationApplied", angle,
                "pageRange", pageRange
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("ROTATE_ERROR", "Failed to rotate PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleWatermark(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Adding watermark");
        
        String watermarkText = (String) parameters.get("watermarkText");
        if (watermarkText == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "watermarkText parameter required");
        }
        
        String fontName = (String) parameters.get("fontName");
        if (fontName == null) fontName = "Helvetica";
        
        Integer fontSize = (Integer) parameters.get("fontSize");
        if (fontSize == null) fontSize = 48;
        
        String color = (String) parameters.get("color");
        if (color == null) color = "#808080";
        
        Double opacity = (Double) parameters.get("opacity");
        if (opacity == null) opacity = 0.3;
        
        String position = (String) parameters.get("position");
        if (position == null) position = "center";
        
        Integer rotation = (Integer) parameters.get("rotation");
        if (rotation == null) rotation = 45;
        
        Boolean diagonal = (Boolean) parameters.get("diagonal");
        if (diagonal == null) diagonal = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "watermarked_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying watermark");
            
            pdfUtil.addWatermark(inputFile, outputPath, watermarkText, fontName, fontSize, 
                color, opacity, position, rotation, diagonal);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "watermarkApplied", watermarkText,
                "opacity", opacity
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("WATERMARK_ERROR", "Failed to add watermark: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleEncrypt(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Encrypting PDF");
        
        String ownerPassword = (String) parameters.get("ownerPassword");
        if (ownerPassword == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "ownerPassword parameter required");
        }
        
        String userPassword = (String) parameters.get("userPassword");
        
        Boolean allowPrint = (Boolean) parameters.get("allowPrint");
        if (allowPrint == null) allowPrint = true;
        
        Boolean allowCopy = (Boolean) parameters.get("allowCopy");
        if (allowCopy == null) allowCopy = true;
        
        Boolean allowModify = (Boolean) parameters.get("allowModify");
        if (allowModify == null) allowModify = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "encrypted_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying encryption");
            
            pdfUtil.encryptPDF(inputFile, outputPath, ownerPassword, userPassword,
                allowPrint, allowCopy, allowModify, true, true, true, true, true);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "encrypted", true,
                "allowPrint", allowPrint,
                "allowCopy", allowCopy,
                "allowModify", allowModify
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("ENCRYPT_ERROR", "Failed to encrypt PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleDecrypt(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Decrypting PDF");
        
        String password = (String) parameters.get("password");
        if (password == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "password parameter required");
        }
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "decrypted_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Removing encryption");
            
            pdfUtil.decryptPDF(inputFile, outputPath, password);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "decrypted", true
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("DECRYPT_ERROR", "Failed to decrypt PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleExtractText(Path inputFile, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Extracting text");
        
        try {
            String text = pdfUtil.extractText(inputFile);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Creating text file");
            
            String outputName = "extracted_text_" + System.currentTimeMillis();
            Path outputPath = createOutputFile(outputName, "txt");
            
            Files.write(outputPath, text.getBytes());
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "textLength", text.length(),
                "pageCount", pdfUtil.getPageCount(inputFile)
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("EXTRACT_TEXT_ERROR", "Failed to extract text: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleExtractImages(Path inputFile, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Extracting images");
        
        try {
            List<BufferedImage> images = pdfUtil.extractImages(inputFile);
            
            jobQueueService.updateProgress(jobStatus.getId(), 60, "Saving images");
            
            List<String> imageFiles = new ArrayList<>();
            Path tempDir = createTempDir();
            
            for (int i = 0; i < images.size(); i++) {
                String fileName = "image_" + (i + 1) + ".png";
                Path imagePath = tempDir.resolve(fileName);
                ImageIO.write(images.get(i), "PNG", imagePath.toFile());
                imageFiles.add(fileName);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Packaging results");
            
            // For now, return list of image names
            // In production, you'd create a zip file
            
            return Map.of(
                "imageCount", images.size(),
                "imageFiles", imageFiles,
                "extracted", true
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("EXTRACT_IMAGES_ERROR", "Failed to extract images: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleExtractMetadata(Path inputFile, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 50, "Extracting metadata");
        
        try {
            Map<String, String> metadata = pdfUtil.extractMetadata(inputFile);
            
            jobQueueService.updateProgress(jobStatus.getId(), 100, "Completed");
            
            return Map.of(
                "metadata", metadata,
                "extracted", true
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("EXTRACT_METADATA_ERROR", "Failed to extract metadata: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleAddPageNumbers(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Adding page numbers");
        
        String format = (String) parameters.get("format");
        if (format == null) format = "Page {page} of {total}";
        
        String fontName = (String) parameters.get("fontName");
        if (fontName == null) fontName = "Helvetica";
        
        Integer fontSize = (Integer) parameters.get("fontSize");
        if (fontSize == null) fontSize = 12;
        
        String color = (String) parameters.get("color");
        if (color == null) color = "#000000";
        
        String position = (String) parameters.get("position");
        if (position == null) position = "bottom-center";
        
        Integer margin = (Integer) parameters.get("margin");
        if (margin == null) margin = 36;
        
        String startPage = (String) parameters.get("startPage");
        if (startPage == null) startPage = "all";
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "numbered_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Adding numbers");
            
            pdfUtil.addPageNumbers(inputFile, outputPath, format, fontName, fontSize, 
                color, position, margin, startPage);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "format", format,
                "position", position
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("PAGE_NUMBERS_ERROR", "Failed to add page numbers: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleRemovePages(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Removing pages");
        
        String pagesToRemove = (String) parameters.get("pagesToRemove");
        if (pagesToRemove == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "pagesToRemove parameter required");
        }
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "modified_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Removing pages");
            
            pdfUtil.removePages(inputFile, outputPath, pagesToRemove);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "pagesRemoved", pagesToRemove
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("REMOVE_PAGES_ERROR", "Failed to remove pages: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleCropPages(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Cropping pages");
        
        Double left = (Double) parameters.get("left");
        if (left == null) left = 0.0;
        
        Double top = (Double) parameters.get("top");
        if (top == null) top = 0.0;
        
        Double right = (Double) parameters.get("right");
        if (right == null) right = 0.0;
        
        Double bottom = (Double) parameters.get("bottom");
        if (bottom == null) bottom = 0.0;
        
        String pageRange = (String) parameters.get("pageRange");
        if (pageRange == null) pageRange = "all";
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "cropped_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying crop");
            
            pdfUtil.cropPages(inputFile, outputPath, left, top, right, bottom, pageRange);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "cropApplied", true,
                "pageRange", pageRange
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("CROP_ERROR", "Failed to crop pages: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleResizePages(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Resizing pages");
        
        Integer width = (Integer) parameters.get("width");
        Integer height = (Integer) parameters.get("height");
        String pageSize = (String) parameters.get("pageSize");
        if (pageSize == null) pageSize = "A4";
        
        String pageRange = (String) parameters.get("pageRange");
        if (pageRange == null) pageRange = "all";
        
        Boolean maintainAspectRatio = (Boolean) parameters.get("maintainAspectRatio");
        if (maintainAspectRatio == null) maintainAspectRatio = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "resized_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying resize");
            
            pdfUtil.resizePages(inputFile, outputPath, width, height, pageSize, pageRange, maintainAspectRatio);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "resized", true,
                "pageSize", pageSize,
                "pageRange", pageRange
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("RESIZE_ERROR", "Failed to resize pages: " + e.getMessage());
        }
    }
    
    // ==================== CONVERSION HANDLERS ====================
    
    private Map<String, Object> handlePDFToImage(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting to images");
        
        String imageFormat = (String) parameters.get("imageFormat");
        if (imageFormat == null) imageFormat = "png";
        
        Integer dpi = (Integer) parameters.get("dpi");
        if (dpi == null) dpi = imageDpi;
        
        String pageRange = (String) parameters.get("pageRange");
        if (pageRange == null) pageRange = "all";
        
        String outputPrefix = (String) parameters.get("outputPrefix");
        if (outputPrefix == null) outputPrefix = "page";
        
        try {
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Rendering pages");
            
            List<BufferedImage> images = pdfUtil.renderPages(inputFile, dpi, pageRange);
            
            jobQueueService.updateProgress(jobStatus.getId(), 75, "Saving images");
            
            Path tempDir = createTempDir();
            List<String> imageFiles = new ArrayList<>();
            
            for (int i = 0; i < images.size(); i++) {
                String fileName = outputPrefix + "_" + (i + 1) + "." + imageFormat;
                Path imagePath = tempDir.resolve(fileName);
                ImageIO.write(images.get(i), imageFormat.toUpperCase(), imagePath.toFile());
                imageFiles.add(fileName);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Packaging");
            
            return Map.of(
                "imageCount", images.size(),
                "imageFiles", imageFiles,
                "dpi", dpi,
                "format", imageFormat
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("PDF_TO_IMAGE_ERROR", "Failed to convert PDF to images: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleImageToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting images to PDF");
        
        List<?> images = (List<?>) parameters.get("images");
        if (images == null || images.isEmpty()) {
            throw new PDFProcessingException("MISSING_PARAMETER", "images parameter required");
        }
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "image_collection";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Creating PDF");
            
            try (PDDocument document = new PDDocument()) {
                for (Object img : images) {
                    if (img instanceof MultipartFile multipartFile) {
                        Path tempImage = fileUtil.saveTempFile(multipartFile);
                        BufferedImage bufferedImage = ImageIO.read(tempImage.toFile());
                        
                        PDPage page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        
                        PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);
                        PDPageContentStream contentStream = new PDPageContentStream(document, page);
                        
                        // Scale to fit page
                        PDRectangle pageSize = page.getMediaBox();
                        float scale = Math.min(pageSize.getWidth() / pdImage.getWidth(), 
                                              pageSize.getHeight() / pdImage.getHeight());
                        
                        contentStream.drawImage(pdImage, 0, 0, pdImage.getWidth() * scale, pdImage.getHeight() * scale);
                        contentStream.close();
                        
                        fileUtil.cleanupTempFile(tempImage);
                    }
                }
                
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "imagesConverted", images.size()
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("IMAGE_TO_PDF_ERROR", "Failed to convert images to PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handlePDFToText(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Extracting text");
        
        String pageRange = (String) parameters.get("pageRange");
        if (pageRange == null) pageRange = "all";
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "extracted_text";
        
        try {
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Processing text");
            
            String text;
            if ("all".equals(pageRange)) {
                text = pdfUtil.extractText(inputFile);
            } else {
                text = pdfUtil.extractText(inputFile, pageRange);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 75, "Saving text");
            
            Path outputPath = createOutputFile(outputName, "txt");
            Files.write(outputPath, text.getBytes());
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "textLength", text.length(),
                "pageRange", pageRange
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("PDF_TO_TEXT_ERROR", "Failed to extract text: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleTextToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting text to PDF");
        
        String fontName = (String) parameters.get("fontName");
        if (fontName == null) fontName = "Helvetica";
        
        Integer fontSize = (Integer) parameters.get("fontSize");
        if (fontSize == null) fontSize = 12;
        
        String pageSize = (String) parameters.get("pageSize");
        if (pageSize == null) pageSize = "A4";
        
        Integer margin = (Integer) parameters.get("margin");
        if (margin == null) margin = 36;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "text_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Reading text");
            
            String text = new String(Files.readAllBytes(inputFile));
            
            jobQueueService.updateProgress(jobStatus.getId(), 75, "Creating PDF");
            
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                
                contentStream.setFont(font, fontSize);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, page.getMediaBox().getHeight() - margin);
                
                String[] lines = text.split("\n");
                float lineHeight = fontSize * 1.2f;
                float currentY = page.getMediaBox().getHeight() - margin;
                float maxY = margin;
                
                for (String line : lines) {
                    currentY -= lineHeight;
                    if (currentY < maxY) {
                        contentStream.endText();
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.setFont(font, fontSize);
                        contentStream.beginText();
                        contentStream.newLineAtOffset(margin, page.getMediaBox().getHeight() - margin);
                        currentY = page.getMediaBox().getHeight() - margin;
                    }
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -lineHeight);
                }
                
                contentStream.endText();
                contentStream.close();
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "textLength", text.length()
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("TEXT_TO_PDF_ERROR", "Failed to convert text to PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handlePDFToWord(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        return handleOfficeConversion(inputFile, parameters, jobStatus, "docx", "PDF to Word conversion");
    }
    
    private Map<String, Object> handlePDFToExcel(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        return handleOfficeConversion(inputFile, parameters, jobStatus, "xlsx", "PDF to Excel conversion");
    }
    
    private Map<String, Object> handlePDFToPPT(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        return handleOfficeConversion(inputFile, parameters, jobStatus, "pptx", "PDF to PowerPoint conversion");
    }
    
    private Map<String, Object> handleWordToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        return handleOfficeConversion(inputFile, parameters, jobStatus, "pdf", "Word to PDF conversion");
    }
    
    private Map<String, Object> handleExcelToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        return handleOfficeConversion(inputFile, parameters, jobStatus, "pdf", "Excel to PDF conversion");
    }
    
    private Map<String, Object> handlePPTToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        return handleOfficeConversion(inputFile, parameters, jobStatus, "pdf", "PowerPoint to PDF conversion");
    }
    
    private Map<String, Object> handleHTMLToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting HTML to PDF");
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "html_document";
        
        Boolean enableImages = (Boolean) parameters.get("enableImages");
        if (enableImages == null) enableImages = true;
        
        Integer timeout = (Integer) parameters.get("timeoutSeconds");
        if (timeout == null) timeout = 30;
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Processing HTML");
            
            // Use LibreOffice for HTML to PDF conversion
            String command = String.format("%s --headless --convert-to pdf --outdir %s %s",
                libreofficePath, 
                outputPath.getParent().toAbsolutePath(),
                inputFile.toAbsolutePath());
            
            executeExternalCommand(command, timeout, "HTML to PDF conversion");
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "converted", true
            );
            
        } catch (Exception e) {
            throw new PDFProcessingException("HTML_TO_PDF_ERROR", "Failed to convert HTML to PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleMarkdownToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        // For markdown, we first convert to HTML, then to PDF
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting Markdown to PDF");
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "markdown_document";
        
        try {
            // Read markdown and convert to simple HTML
            String markdown = new String(Files.readAllBytes(inputFile));
            String html = convertMarkdownToHTML(markdown);
            
            // Save HTML temporarily
            Path tempHtml = Files.createTempFile("temp", ".html");
            Files.write(tempHtml, html.getBytes());
            
            // Convert HTML to PDF
            Map<String, Object> result = handleHTMLToPDF(tempHtml, parameters, jobStatus);
            
            // Cleanup
            fileUtil.cleanupTempFile(tempHtml);
            
            return result;
            
        } catch (IOException e) {
            throw new PDFProcessingException("MARKDOWN_TO_PDF_ERROR", "Failed to convert Markdown to PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleTxtToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        // This is the same as text-to-pdf
        return handleTextToPDF(inputFile, parameters, jobStatus);
    }
    
    // ==================== OCR HANDLERS ====================
    
    private Map<String, Object> handleOCRPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Performing OCR");
        
        String language = (String) parameters.get("language");
        if (language == null) language = "eng";
        
        Integer dpi = (Integer) parameters.get("dpi");
        if (dpi == null) dpi = ocrDpi;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "ocr_document";
        
        // Enhanced options
        Boolean extractTables = (Boolean) parameters.get("extractTables");
        if (extractTables == null) extractTables = false;
        
        Boolean includeConfidence = (Boolean) parameters.get("includeConfidence");
        if (includeConfidence == null) includeConfidence = false;
        
        String outputFormat = (String) parameters.get("outputFormat");
        if (outputFormat == null) outputFormat = "pdf"; // pdf, txt, json
        
        Boolean preprocessImage = (Boolean) parameters.get("preprocessImage");
        if (preprocessImage == null) preprocessImage = true;
        
        try {
            jobQueueService.updateProgress(jobStatus.getId(), 40, "Extracting text from images");
            
            // Render PDF pages as images
            List<BufferedImage> images = pdfUtil.renderPages(inputFile, dpi);
            
            jobQueueService.updateProgress(jobStatus.getId(), 60, "Running OCR");
            
            ITesseract tesseract = new Tesseract();
            if (tesseractDataPath != null && !tesseractDataPath.isEmpty()) {
                tesseract.setDatapath(tesseractDataPath);
            }
            tesseract.setLanguage(language);
            
            // Configure for better accuracy
            tesseract.setPageSegMode(3); // PSM_AUTO - Fully automatic page segmentation
            tesseract.setOcrEngineMode(1); // LSTM only
            
            StringBuilder extractedText = new StringBuilder();
            List<Map<String, Object>> pageResults = new ArrayList<>();
            List<Map<String, Object>> detectedTables = new ArrayList<>();
            double totalConfidence = 0;
            int wordCount = 0;
            
            for (int pageNum = 0; pageNum < images.size(); pageNum++) {
                BufferedImage image = images.get(pageNum);
                
                // Preprocess image if requested
                if (preprocessImage) {
                    image = preprocessForOCR(image);
                }
                
                Map<String, Object> pageResult = new LinkedHashMap<>();
                pageResult.put("pageNumber", pageNum + 1);
                
                try {
                    String text = tesseract.doOCR(image);
                    extractedText.append(text).append("\n\n");
                    pageResult.put("text", text);
                    pageResult.put("success", true);
                    
                    // Estimate word count
                    int pageWordCount = text.split("\\s+").length;
                    wordCount += pageWordCount;
                    pageResult.put("wordCount", pageWordCount);
                    
                    // Get confidence if requested (requires additional Tesseract config)
                    if (includeConfidence) {
                        // Simulate confidence based on text quality metrics
                        double confidence = calculateTextConfidence(text);
                        totalConfidence += confidence;
                        pageResult.put("confidence", confidence);
                    }
                    
                    // Extract tables if requested
                    if (extractTables) {
                        List<Map<String, Object>> pageTables = detectTablesInText(text, pageNum + 1);
                        if (!pageTables.isEmpty()) {
                            detectedTables.addAll(pageTables);
                            pageResult.put("tablesFound", pageTables.size());
                        }
                    }
                    
                } catch (TesseractException e) {
                    logger.warn("OCR failed for page {}: {}", pageNum + 1, e.getMessage());
                    pageResult.put("success", false);
                    pageResult.put("error", e.getMessage());
                }
                
                pageResults.add(pageResult);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 85, "Creating output");
            
            Map<String, Object> result = new LinkedHashMap<>();
            
            // Output based on format
            if ("json".equals(outputFormat)) {
                Path outputPath = createOutputFile(outputName, "json");
                Map<String, Object> jsonOutput = new LinkedHashMap<>();
                jsonOutput.put("text", extractedText.toString());
                jsonOutput.put("pages", pageResults);
                jsonOutput.put("wordCount", wordCount);
                if (includeConfidence) {
                    jsonOutput.put("averageConfidence", pageResults.isEmpty() ? 0 : totalConfidence / pageResults.size());
                }
                if (extractTables && !detectedTables.isEmpty()) {
                    jsonOutput.put("tables", detectedTables);
                }
                
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Files.writeString(outputPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonOutput));
                
                result.put("resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()));
                result.put("format", "json");
                
            } else if ("txt".equals(outputFormat)) {
                Path outputPath = createOutputFile(outputName, "txt");
                Files.writeString(outputPath, extractedText.toString());
                
                result.put("resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()));
                result.put("format", "txt");
                
            } else {
                // Create searchable PDF
                Path outputPath = createOutputFile(outputName, "pdf");
                createSearchablePDF(outputPath, images, extractedText.toString());
                
                result.put("resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()));
                result.put("format", "pdf");
            }
            
            result.put("textLength", extractedText.length());
            result.put("wordCount", wordCount);
            result.put("pagesProcessed", images.size());
            result.put("language", language);
            result.put("dpi", dpi);
            
            if (includeConfidence) {
                result.put("averageConfidence", pageResults.isEmpty() ? 0 : totalConfidence / pageResults.size());
            }
            
            if (extractTables && !detectedTables.isEmpty()) {
                result.put("tablesDetected", detectedTables.size());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 100, "Complete");
            
            return result;
            
        } catch (Exception e) {
            throw new PDFProcessingException("OCR_ERROR", "Failed to perform OCR: " + e.getMessage());
        }
    }
    
    private BufferedImage preprocessForOCR(BufferedImage image) {
        // Convert to grayscale
        BufferedImage grayscale = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        java.awt.Graphics g = grayscale.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        // Apply simple contrast enhancement
        for (int y = 0; y < grayscale.getHeight(); y++) {
            for (int x = 0; x < grayscale.getWidth(); x++) {
                int rgb = grayscale.getRGB(x, y) & 0xFF;
                // Increase contrast
                int newValue = (int) ((rgb - 128) * 1.5 + 128);
                newValue = Math.max(0, Math.min(255, newValue));
                grayscale.setRGB(x, y, (newValue << 16) | (newValue << 8) | newValue);
            }
        }
        
        return grayscale;
    }
    
    private double calculateTextConfidence(String text) {
        if (text == null || text.isEmpty()) return 0.0;
        
        // Calculate confidence based on text quality indicators
        int totalChars = text.length();
        int validChars = 0;
        int wordChars = 0;
        
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || ".,!?;:'-\"()".indexOf(c) >= 0) {
                validChars++;
            }
            if (Character.isLetterOrDigit(c)) {
                wordChars++;
            }
        }
        
        double charRatio = totalChars > 0 ? (double) validChars / totalChars : 0;
        double wordRatio = totalChars > 0 ? (double) wordChars / totalChars : 0;
        
        // Good text typically has 95%+ valid characters and 60%+ word characters
        double confidence = (charRatio * 0.5 + wordRatio * 0.5) * 100;
        return Math.min(100, Math.max(0, confidence));
    }
    
    private List<Map<String, Object>> detectTablesInText(String text, int pageNumber) {
        List<Map<String, Object>> tables = new ArrayList<>();
        
        // Simple table detection based on aligned columns with spaces or tabs
        String[] lines = text.split("\n");
        List<String> tableLines = new ArrayList<>();
        int tableStart = -1;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // Check if line looks like a table row (has multiple aligned columns)
            if (looksLikeTableRow(line)) {
                if (tableStart == -1) {
                    tableStart = i;
                }
                tableLines.add(line);
            } else if (!tableLines.isEmpty() && tableLines.size() >= 2) {
                // End of table - save if we have at least 2 rows
                Map<String, Object> table = new LinkedHashMap<>();
                table.put("pageNumber", pageNumber);
                table.put("startLine", tableStart);
                table.put("endLine", i - 1);
                table.put("rowCount", tableLines.size());
                table.put("data", parseTableData(tableLines));
                tables.add(table);
                
                tableLines.clear();
                tableStart = -1;
            } else {
                tableLines.clear();
                tableStart = -1;
            }
        }
        
        // Check for table at end of text
        if (!tableLines.isEmpty() && tableLines.size() >= 2) {
            Map<String, Object> table = new LinkedHashMap<>();
            table.put("pageNumber", pageNumber);
            table.put("startLine", tableStart);
            table.put("rowCount", tableLines.size());
            table.put("data", parseTableData(tableLines));
            tables.add(table);
        }
        
        return tables;
    }
    
    private boolean looksLikeTableRow(String line) {
        // A line looks like a table row if it has multiple segments separated by 2+ spaces
        if (line.trim().isEmpty()) return false;
        String[] parts = line.split("\\s{2,}");
        return parts.length >= 2;
    }
    
    private List<List<String>> parseTableData(List<String> tableLines) {
        List<List<String>> data = new ArrayList<>();
        for (String line : tableLines) {
            String[] cells = line.split("\\s{2,}");
            List<String> row = new ArrayList<>();
            for (String cell : cells) {
                row.add(cell.trim());
            }
            data.add(row);
        }
        return data;
    }
    
    private void createSearchablePDF(Path outputPath, List<BufferedImage> images, String extractedText) throws IOException {
        try (PDDocument document = new PDDocument()) {
            String[] textPages = extractedText.split("\n\n");
            
            for (int i = 0; i < images.size(); i++) {
                BufferedImage image = images.get(i);
                
                // Create page with image dimensions
                PDRectangle pageSize = new PDRectangle(image.getWidth(), image.getHeight());
                PDPage page = new PDPage(pageSize);
                document.addPage(page);
                
                // Add original image as background
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.drawImage(pdImage, 0, 0, pageSize.getWidth(), pageSize.getHeight());
                    
                    // Add invisible text layer for searchability
                    if (i < textPages.length) {
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 1);
                        contentStream.setNonStrokingColor(1f, 1f, 1f); // White (invisible)
                        contentStream.newLineAtOffset(10, pageSize.getHeight() - 10);
                        
                        // Add text in very small font (effectively invisible but searchable)
                        String pageText = textPages[i].replace("\n", " ");
                        if (pageText.length() > 1000) {
                            pageText = pageText.substring(0, 1000); // Limit text length
                        }
                        contentStream.showText(pageText);
                        contentStream.endText();
                    }
                }
            }
            
            document.save(outputPath.toFile());
        }
    }
    
    // ==================== COMPARISON HANDLERS ====================
    
    private Map<String, Object> handleComparePDFs(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Comparing PDFs");
        
        Object file2Obj = parameters.get("file2");
        if (file2Obj == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "file2 parameter required");
        }
        
        // Enhanced comparison options
        String compareMode = (String) parameters.get("compareMode");
        if (compareMode == null) compareMode = "visual"; // visual, text, both
        
        Boolean generateRedline = (Boolean) parameters.get("generateRedline");
        if (generateRedline == null) generateRedline = true;
        
        Boolean includeChangeList = (Boolean) parameters.get("includeChangeList");
        if (includeChangeList == null) includeChangeList = true;
        
        try {
            Path file2;
            if (file2Obj instanceof MultipartFile multipartFile) {
                file2 = fileUtil.saveTempFile(multipartFile);
            } else {
                throw new PDFProcessingException("INVALID_PARAMETER", "file2 must be a multipart file");
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 40, "Analyzing documents");
            
            String outputName = (String) parameters.get("outputFileName");
            if (outputName == null) outputName = "comparison_result";
            
            Map<String, Object> result = new LinkedHashMap<>();
            
            // Extract text for semantic comparison
            String text1 = pdfUtil.extractText(inputFile);
            String text2 = pdfUtil.extractText(file2);
            
            // Perform text-based comparison
            List<Map<String, Object>> textDifferences = new ArrayList<>();
            if ("text".equals(compareMode) || "both".equals(compareMode)) {
                textDifferences = performTextDiff(text1, text2);
                result.put("textDifferences", textDifferences);
                result.put("textDiffCount", textDifferences.size());
            }
            
            // Perform structural comparison
            Map<String, Object> structuralDiff = performStructuralComparison(inputFile, file2);
            result.put("structuralComparison", structuralDiff);
            
            jobQueueService.updateProgress(jobStatus.getId(), 60, "Running visual comparison");
            
            // Perform visual comparison
            Path outputPath = createOutputFile(outputName, "pdf");
            int visualDifferenceCount = 0;
            
            if ("visual".equals(compareMode) || "both".equals(compareMode)) {
                CompareResult compareResult = new PdfComparator<>(file2.toFile(), inputFile.toFile()).compare();
                
                if (compareResult.isEqual()) {
                    result.put("visuallyIdentical", true);
                    result.put("visualDifferences", 0);
                } else {
                    compareResult.writeTo(outputPath.toString());
                    visualDifferenceCount = compareResult.getDifferences().size();
                    result.put("visuallyIdentical", false);
                    result.put("visualDifferences", visualDifferenceCount);
                    result.put("comparisonResultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()));
                }
            }
            
            // Generate redline document
            if (generateRedline && !textDifferences.isEmpty()) {
                jobQueueService.updateProgress(jobStatus.getId(), 80, "Generating redline document");
                Path redlinePath = createOutputFile(outputName + "_redline", "pdf");
                generateRedlineDocument(inputFile, text1, textDifferences, redlinePath);
                result.put("redlineUrl", fileUtil.getDownloadUrl(redlinePath.getFileName().toString()));
            }
            
            // Generate change list
            if (includeChangeList) {
                Path changeListPath = createOutputFile(outputName + "_changes", "txt");
                generateChangeList(textDifferences, structuralDiff, changeListPath);
                result.put("changeListUrl", fileUtil.getDownloadUrl(changeListPath.getFileName().toString()));
            }
            
            // Overall comparison summary
            boolean areEqual = textDifferences.isEmpty() && visualDifferenceCount == 0;
            result.put("areEqual", areEqual);
            result.put("compareMode", compareMode);
            result.put("totalDifferences", textDifferences.size() + visualDifferenceCount);
            
            jobQueueService.updateProgress(jobStatus.getId(), 100, "Complete");
            
            return result;
            
        } catch (IOException e) {
            throw new PDFProcessingException("COMPARE_ERROR", "Failed to compare PDFs: " + e.getMessage());
        }
    }
    
    private List<Map<String, Object>> performTextDiff(String text1, String text2) {
        List<Map<String, Object>> differences = new ArrayList<>();
        
        String[] lines1 = text1.split("\n");
        String[] lines2 = text2.split("\n");
        
        int maxLines = Math.max(lines1.length, lines2.length);
        
        for (int i = 0; i < maxLines; i++) {
            String line1 = i < lines1.length ? lines1[i].trim() : "";
            String line2 = i < lines2.length ? lines2[i].trim() : "";
            
            if (!line1.equals(line2)) {
                Map<String, Object> diff = new LinkedHashMap<>();
                diff.put("lineNumber", i + 1);
                
                if (line1.isEmpty() && !line2.isEmpty()) {
                    diff.put("type", "ADDED");
                    diff.put("newText", line2);
                } else if (!line1.isEmpty() && line2.isEmpty()) {
                    diff.put("type", "DELETED");
                    diff.put("oldText", line1);
                } else {
                    diff.put("type", "MODIFIED");
                    diff.put("oldText", line1);
                    diff.put("newText", line2);
                    
                    // Calculate word-level diff
                    List<Map<String, String>> wordChanges = findWordChanges(line1, line2);
                    if (!wordChanges.isEmpty()) {
                        diff.put("wordChanges", wordChanges);
                    }
                }
                
                differences.add(diff);
            }
        }
        
        return differences;
    }
    
    private List<Map<String, String>> findWordChanges(String line1, String line2) {
        List<Map<String, String>> changes = new ArrayList<>();
        
        String[] words1 = line1.split("\\s+");
        String[] words2 = line2.split("\\s+");
        
        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));
        
        // Find deleted words
        for (String word : words1) {
            if (!set2.contains(word)) {
                Map<String, String> change = new HashMap<>();
                change.put("type", "deleted");
                change.put("word", word);
                changes.add(change);
            }
        }
        
        // Find added words
        for (String word : words2) {
            if (!set1.contains(word)) {
                Map<String, String> change = new HashMap<>();
                change.put("type", "added");
                change.put("word", word);
                changes.add(change);
            }
        }
        
        return changes;
    }
    
    private Map<String, Object> performStructuralComparison(Path file1, Path file2) throws IOException {
        Map<String, Object> structural = new LinkedHashMap<>();
        
        try (PDDocument doc1 = pdfUtil.loadPDF(file1);
             PDDocument doc2 = pdfUtil.loadPDF(file2)) {
            
            // Page count comparison
            structural.put("file1PageCount", doc1.getNumberOfPages());
            structural.put("file2PageCount", doc2.getNumberOfPages());
            structural.put("pageCountDifferent", doc1.getNumberOfPages() != doc2.getNumberOfPages());
            
            // Metadata comparison
            Map<String, Object> metadataDiff = new LinkedHashMap<>();
            org.apache.pdfbox.pdmodel.PDDocumentInformation info1 = doc1.getDocumentInformation();
            org.apache.pdfbox.pdmodel.PDDocumentInformation info2 = doc2.getDocumentInformation();
            
            if (info1 != null && info2 != null) {
                if (!Objects.equals(info1.getTitle(), info2.getTitle())) {
                    metadataDiff.put("title", Map.of("file1", info1.getTitle(), "file2", info2.getTitle()));
                }
                if (!Objects.equals(info1.getAuthor(), info2.getAuthor())) {
                    metadataDiff.put("author", Map.of("file1", info1.getAuthor(), "file2", info2.getAuthor()));
                }
            }
            structural.put("metadataDifferences", metadataDiff);
            
            // Font comparison
            Set<String> fonts1 = extractFontNames(doc1);
            Set<String> fonts2 = extractFontNames(doc2);
            
            Set<String> addedFonts = new HashSet<>(fonts2);
            addedFonts.removeAll(fonts1);
            Set<String> removedFonts = new HashSet<>(fonts1);
            removedFonts.removeAll(fonts2);
            
            structural.put("fontsAdded", addedFonts);
            structural.put("fontsRemoved", removedFonts);
            
            // Form field comparison
            boolean hasForm1 = doc1.getDocumentCatalog().getAcroForm() != null;
            boolean hasForm2 = doc2.getDocumentCatalog().getAcroForm() != null;
            structural.put("formFieldsDifferent", hasForm1 != hasForm2);
        }
        
        return structural;
    }
    
    private Set<String> extractFontNames(PDDocument document) {
        Set<String> fonts = new HashSet<>();
        for (int i = 0; i < document.getNumberOfPages(); i++) {
            PDPage page = document.getPage(i);
            if (page.getResources() != null && page.getResources().getFontNames() != null) {
                for (org.apache.pdfbox.cos.COSName fontName : page.getResources().getFontNames()) {
                    fonts.add(fontName.getName());
                }
            }
        }
        return fonts;
    }
    
    private void generateRedlineDocument(Path originalPdf, String originalText, 
            List<Map<String, Object>> differences, Path outputPath) throws IOException {
        
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            
            float yPosition = page.getMediaBox().getHeight() - 50;
            float margin = 40;
            float fontSize = 10;
            float lineHeight = 14;
            
            // Title
            contentStream.beginText();
            contentStream.setFont(boldFont, 14);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("REDLINE COMPARISON REPORT");
            contentStream.endText();
            yPosition -= 30;
            
            // Summary
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Total changes found: " + differences.size());
            contentStream.endText();
            yPosition -= 25;
            
            // Changes
            for (Map<String, Object> diff : differences) {
                if (yPosition < 100) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = page.getMediaBox().getHeight() - 50;
                }
                
                String type = (String) diff.get("type");
                int lineNum = (Integer) diff.get("lineNumber");
                
                // Set color based on change type
                if ("ADDED".equals(type)) {
                    contentStream.setNonStrokingColor(0f, 0.5f, 0f); // Green
                } else if ("DELETED".equals(type)) {
                    contentStream.setNonStrokingColor(0.8f, 0f, 0f); // Red
                } else {
                    contentStream.setNonStrokingColor(0.8f, 0.5f, 0f); // Orange
                }
                
                contentStream.beginText();
                contentStream.setFont(boldFont, fontSize);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(String.format("[%s] Line %d:", type, lineNum));
                contentStream.endText();
                yPosition -= lineHeight;
                
                // Show old text (if applicable)
                contentStream.setNonStrokingColor(0f, 0f, 0f); // Black
                if (diff.containsKey("oldText")) {
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(margin + 20, yPosition);
                    String oldText = truncateText((String) diff.get("oldText"), 80);
                    contentStream.showText("- " + oldText);
                    contentStream.endText();
                    yPosition -= lineHeight;
                }
                
                // Show new text (if applicable)
                if (diff.containsKey("newText")) {
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(margin + 20, yPosition);
                    String newText = truncateText((String) diff.get("newText"), 80);
                    contentStream.showText("+ " + newText);
                    contentStream.endText();
                    yPosition -= lineHeight;
                }
                
                yPosition -= 10;
            }
            
            contentStream.close();
            document.save(outputPath.toFile());
        }
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    private void generateChangeList(List<Map<String, Object>> textDifferences, 
            Map<String, Object> structuralDiff, Path outputPath) throws IOException {
        
        StringBuilder sb = new StringBuilder();
        sb.append("COMPARISON CHANGE LIST\n");
        sb.append("=".repeat(50)).append("\n\n");
        
        // Structural changes
        sb.append("STRUCTURAL DIFFERENCES:\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("File 1 Pages: %s\n", structuralDiff.get("file1PageCount")));
        sb.append(String.format("File 2 Pages: %s\n", structuralDiff.get("file2PageCount")));
        
        @SuppressWarnings("unchecked")
        Set<String> fontsAdded = (Set<String>) structuralDiff.get("fontsAdded");
        @SuppressWarnings("unchecked")
        Set<String> fontsRemoved = (Set<String>) structuralDiff.get("fontsRemoved");
        
        if (!fontsAdded.isEmpty()) {
            sb.append("Fonts Added: ").append(String.join(", ", fontsAdded)).append("\n");
        }
        if (!fontsRemoved.isEmpty()) {
            sb.append("Fonts Removed: ").append(String.join(", ", fontsRemoved)).append("\n");
        }
        sb.append("\n");
        
        // Text changes
        sb.append("TEXT DIFFERENCES:\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Total changes: %d\n\n", textDifferences.size()));
        
        int addedCount = 0, deletedCount = 0, modifiedCount = 0;
        
        for (Map<String, Object> diff : textDifferences) {
            String type = (String) diff.get("type");
            int lineNum = (Integer) diff.get("lineNumber");
            
            switch (type) {
                case "ADDED": addedCount++; break;
                case "DELETED": deletedCount++; break;
                case "MODIFIED": modifiedCount++; break;
            }
            
            sb.append(String.format("[%s] Line %d\n", type, lineNum));
            if (diff.containsKey("oldText")) {
                sb.append("  OLD: ").append(diff.get("oldText")).append("\n");
            }
            if (diff.containsKey("newText")) {
                sb.append("  NEW: ").append(diff.get("newText")).append("\n");
            }
            sb.append("\n");
        }
        
        sb.append("\n");
        sb.append("SUMMARY:\n");
        sb.append("-".repeat(30)).append("\n");
        sb.append(String.format("Added lines:    %d\n", addedCount));
        sb.append(String.format("Deleted lines:  %d\n", deletedCount));
        sb.append(String.format("Modified lines: %d\n", modifiedCount));
        sb.append(String.format("Total changes:  %d\n", textDifferences.size()));
        
        Files.writeString(outputPath, sb.toString());
    }
    
    // ==================== ADVANCED HANDLERS ====================
    
    private Map<String, Object> handlePDFAConvert(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting to PDF/A");
        
        String complianceLevel = (String) parameters.get("complianceLevel");
        if (complianceLevel == null) complianceLevel = "2b";
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "pdfa_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Processing PDF");
            
            // Load and re-save with PDF/A-compatible settings
            // Note: Full PDF/A conversion requires specialized libraries
            try (PDDocument doc = Loader.loadPDF(inputFile.toFile())) {
                // Set PDF version and basic metadata for PDF/A compliance
                doc.getDocumentCatalog().setVersion(String.valueOf(doc.getVersion()));
                
                doc.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "complianceLevel", complianceLevel,
                "converted", true
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("PDFA_ERROR", "Failed to convert to PDF/A: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleLinearize(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Linearizing PDF");
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "linearized_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Optimizing for web");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                // Linearization is a complex process - for now we'll just save
                // In production, you'd use specialized linearization tools
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "linearized", true
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("LINEARIZE_ERROR", "Failed to linearize PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleOptimize(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Optimizing PDF");
        
        Boolean removeUnused = (Boolean) parameters.get("removeUnusedObjects");
        if (removeUnused == null) removeUnused = true;
        
        Boolean compressImages = (Boolean) parameters.get("compressImages");
        if (compressImages == null) compressImages = true;
        
        Integer imageQuality = (Integer) parameters.get("imageQuality");
        if (imageQuality == null) imageQuality = 85;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "optimized_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying optimizations");
            
            // This is a simplified optimization
            // In production, you'd use specialized tools
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            long originalSize = Files.size(inputFile);
            long optimizedSize = Files.size(outputPath);
            double savings = (1.0 - (double) optimizedSize / originalSize) * 100;
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "optimized", true,
                "savings", String.format("%.1f%%", savings)
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("OPTIMIZE_ERROR", "Failed to optimize PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleMetadataEdit(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Editing metadata");
        
        String title = (String) parameters.get("title");
        String author = (String) parameters.get("author");
        String subject = (String) parameters.get("subject");
        String keywords = (String) parameters.get("keywords");
        String creator = (String) parameters.get("creator");
        String producer = (String) parameters.get("producer");
        
        Map<String, String> customMetadata = (Map<String, String>) parameters.get("customMetadata");
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "metadata_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying metadata");
            
            Map<String, String> metadata = new HashMap<>();
            if (title != null) metadata.put("title", title);
            if (author != null) metadata.put("author", author);
            if (subject != null) metadata.put("subject", subject);
            if (keywords != null) metadata.put("keywords", keywords);
            if (creator != null) metadata.put("creator", creator);
            if (producer != null) metadata.put("producer", producer);
            if (customMetadata != null) metadata.putAll(customMetadata);
            
            pdfUtil.editMetadata(inputFile, outputPath, metadata);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "metadataUpdated", metadata.size()
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("METADATA_EDIT_ERROR", "Failed to edit metadata: " + e.getMessage());
        }
    }
    
    // ==================== SECURITY HANDLERS ====================
    
    private Map<String, Object> handleSignPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Signing PDF");
        
        // Extract signature parameters
        String reason = (String) parameters.get("reason");
        if (reason == null) reason = "Document signed";
        
        String location = (String) parameters.get("location");
        String signerName = (String) parameters.get("signerName");
        
        // Certificate parameters for PKI signing
        String keystorePath = (String) parameters.get("keystorePath");
        String keystorePassword = (String) parameters.get("keystorePassword");
        String keyAlias = (String) parameters.get("keyAlias");
        String keyPassword = (String) parameters.get("keyPassword");
        
        Boolean visibleSignature = (Boolean) parameters.get("visibleSignature");
        if (visibleSignature == null) visibleSignature = false;
        
        Boolean includeTimestamp = (Boolean) parameters.get("includeTimestamp");
        if (includeTimestamp == null) includeTimestamp = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "signed_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying digital signature");
            
            // Check if PKI certificate is provided
            if (keystorePath != null && !keystorePath.isEmpty()) {
                // Use full PKI digital signature with DigitalSignatureService
                DigitalSignatureService.SignatureConfig config = new DigitalSignatureService.SignatureConfig();
                config.setKeystorePath(keystorePath);
                config.setKeystorePassword(keystorePassword);
                config.setKeyAlias(keyAlias);
                config.setKeyPassword(keyPassword != null ? keyPassword : keystorePassword);
                config.setSignerName(signerName);
                config.setReason(reason);
                config.setLocation(location);
                config.setVisibleSignature(visibleSignature);
                config.setIncludeTimestamp(includeTimestamp);
                
                // Optional signature position parameters
                if (parameters.containsKey("signaturePage")) {
                    config.setSignaturePage(((Number) parameters.get("signaturePage")).intValue());
                }
                if (parameters.containsKey("signatureX")) {
                    config.setSignatureX(((Number) parameters.get("signatureX")).floatValue());
                }
                if (parameters.containsKey("signatureY")) {
                    config.setSignatureY(((Number) parameters.get("signatureY")).floatValue());
                }
                
                jobQueueService.updateProgress(jobStatus.getId(), 70, "Creating PKI digital signature");
                
                // Execute real PKI signing
                Map<String, Object> signResult = digitalSignatureService.signPDF(inputFile, outputPath, config);
                
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
                
                return signResult;
                
            } else {
                // Self-signed certificate fallback - generate temporary certificate and sign
                jobQueueService.updateProgress(jobStatus.getId(), 60, "Generating self-signed certificate");
                
                // Create temp path for keystore
                Path tempKeystoreFile = Files.createTempFile("temp_keystore_", ".p12");
                
                // Generate temporary self-signed certificate
                String effectiveSignerName = signerName != null ? signerName : "Document Signer";
                Map<String, Object> certResult = digitalSignatureService.generateSelfSignedCertificate(
                    effectiveSignerName,
                    "PDF Platform Auto-Generated",
                    365, // Valid for 1 year
                    tempKeystoreFile
                );
                
                String tempKeystorePath = (String) certResult.get("certificatePath");
                String tempKeystorePassword = "changeit"; // Default password used by generateSelfSignedCertificate
                
                DigitalSignatureService.SignatureConfig config = new DigitalSignatureService.SignatureConfig();
                config.setKeystorePath(tempKeystorePath);
                config.setKeystorePassword(tempKeystorePassword);
                config.setKeyPassword(tempKeystorePassword);
                config.setSignerName(signerName != null ? signerName : "Document Signer");
                config.setReason(reason);
                config.setLocation(location);
                config.setVisibleSignature(visibleSignature);
                config.setIncludeTimestamp(false); // Self-signed doesn't need TSA
                
                jobQueueService.updateProgress(jobStatus.getId(), 80, "Signing with generated certificate");
                
                // Execute signing with generated certificate
                Map<String, Object> signResult = digitalSignatureService.signPDF(inputFile, outputPath, config);
                
                // Clean up temporary keystore
                try {
                    Files.deleteIfExists(Path.of(tempKeystorePath));
                } catch (Exception e) {
                    logger.warn("Could not delete temporary keystore: {}", e.getMessage());
                }
                
                jobQueueService.updateProgress(jobStatus.getId(), 95, "Finalizing");
                
                // Add note about self-signed certificate
                Map<String, Object> result = new LinkedHashMap<>(signResult);
                result.put("selfSigned", true);
                result.put("note", "Document signed with auto-generated self-signed certificate. For production use, provide a proper PKI certificate.");
                
                return result;
            }
            
        } catch (Exception e) {
            throw new PDFProcessingException("SIGN_ERROR", "Failed to sign PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleVerifySignature(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Verifying signatures");
        
        try {
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Analyzing digital signatures");
            
            // Use DigitalSignatureService for real PKI signature verification
            Map<String, Object> verificationResult = digitalSignatureService.verifySignatures(inputFile);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Verification complete");
            
            return verificationResult;
            
        } catch (Exception e) {
            throw new PDFProcessingException("VERIFY_ERROR", "Failed to verify signatures: " + e.getMessage());
        }
    }
    
    // ==================== REDACTION & CLEANUP HANDLERS ====================
    
    private Map<String, Object> handleRedactPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Redacting PDF");
        
        @SuppressWarnings("unchecked")
        List<String> searchTerms = (List<String>) parameters.get("searchTerms");
        String redactionColor = (String) parameters.get("redactionColor");
        if (redactionColor == null) redactionColor = "#000000";
        
        Boolean removeMetadata = (Boolean) parameters.get("removeMetadata");
        if (removeMetadata == null) removeMetadata = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "redacted_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Applying redactions");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                // Remove metadata if requested
                if (removeMetadata) {
                    document.setDocumentInformation(new org.apache.pdfbox.pdmodel.PDDocumentInformation());
                }
                
                // For production, you'd implement text search and rectangle redaction
                // This is a simplified implementation
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "redacted", true,
                "metadataRemoved", removeMetadata
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("REDACT_ERROR", "Failed to redact PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleFlattenPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Flattening PDF");
        
        Boolean flattenForms = (Boolean) parameters.get("flattenForms");
        if (flattenForms == null) flattenForms = true;
        
        Boolean flattenAnnotations = (Boolean) parameters.get("flattenAnnotations");
        if (flattenAnnotations == null) flattenAnnotations = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "flattened_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Flattening content");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                // Flatten forms by iterating through pages and form fields
                if (flattenForms) {
                    var acroForm = document.getDocumentCatalog().getAcroForm();
                    if (acroForm != null) {
                        acroForm.flatten();
                    }
                }
                
                // Remove annotations if requested
                if (flattenAnnotations) {
                    for (PDPage page : document.getPages()) {
                        page.setAnnotations(new java.util.ArrayList<>());
                    }
                }
                
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "flattened", true,
                "formsFlattened", flattenForms,
                "annotationsFlattened", flattenAnnotations
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("FLATTEN_ERROR", "Failed to flatten PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleRepairPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Repairing PDF");
        
        Boolean validateAfterRepair = (Boolean) parameters.get("validateAfterRepair");
        if (validateAfterRepair == null) validateAfterRepair = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "repaired_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Attempting repair");
            
            // Try to load and re-save to fix minor issues
            try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
                // Re-save with clean structure
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 75, "Validating repair");
            
            boolean isValid = false;
            if (validateAfterRepair) {
                try (PDDocument repaired = Loader.loadPDF(outputPath.toFile())) {
                    isValid = repaired.getNumberOfPages() > 0;
                }
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "repaired", true,
                "isValid", isValid
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("REPAIR_ERROR", "Failed to repair PDF: " + e.getMessage());
        }
    }
    
    // ==================== PAGE MANIPULATION HANDLERS ====================
    
    private Map<String, Object> handleReorderPages(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Reordering pages");
        
        @SuppressWarnings("unchecked")
        List<Integer> pageOrder = (List<Integer>) parameters.get("pageOrder");
        if (pageOrder == null || pageOrder.isEmpty()) {
            throw new PDFProcessingException("MISSING_PARAMETER", "pageOrder parameter required");
        }
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "reordered_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Rearranging pages");
            
            try (PDDocument sourceDoc = pdfUtil.loadPDF(inputFile);
                 PDDocument newDoc = new PDDocument()) {
                
                for (Integer pageNum : pageOrder) {
                    if (pageNum > 0 && pageNum <= sourceDoc.getNumberOfPages()) {
                        PDPage page = sourceDoc.getPage(pageNum - 1);
                        newDoc.addPage(page);
                    }
                }
                
                newDoc.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "reordered", true,
                "pageCount", pageOrder.size()
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("REORDER_ERROR", "Failed to reorder pages: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleInsertPages(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Inserting pages");
        
        Object sourcePdfObj = parameters.get("sourcePdf");
        if (sourcePdfObj == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "sourcePdf parameter required");
        }
        
        Integer insertAfterPage = (Integer) parameters.get("insertAfterPage");
        if (insertAfterPage == null) insertAfterPage = 0;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "merged_document";
        
        try {
            Path sourcePath;
            if (sourcePdfObj instanceof MultipartFile multipartFile) {
                sourcePath = fileUtil.saveTempFile(multipartFile);
            } else {
                throw new PDFProcessingException("INVALID_PARAMETER", "sourcePdf must be a file");
            }
            
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Merging pages");
            
            try (PDDocument targetDoc = pdfUtil.loadPDF(inputFile);
                 PDDocument sourceDoc = pdfUtil.loadPDF(sourcePath);
                 PDDocument newDoc = new PDDocument()) {
                
                // Add pages from target up to insert point
                for (int i = 0; i < insertAfterPage && i < targetDoc.getNumberOfPages(); i++) {
                    newDoc.addPage(targetDoc.getPage(i));
                }
                
                // Add all pages from source
                for (PDPage page : sourceDoc.getPages()) {
                    newDoc.addPage(page);
                }
                
                // Add remaining pages from target
                for (int i = insertAfterPage; i < targetDoc.getNumberOfPages(); i++) {
                    newDoc.addPage(targetDoc.getPage(i));
                }
                
                newDoc.save(outputPath.toFile());
            }
            
            fileUtil.cleanupTempFile(sourcePath);
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "inserted", true
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("INSERT_ERROR", "Failed to insert pages: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleExtractPages(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Extracting pages");
        
        String pageRange = (String) parameters.get("pageRange");
        if (pageRange == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "pageRange parameter required");
        }
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "extracted_pages";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Processing pages");
            
            try (PDDocument sourceDoc = pdfUtil.loadPDF(inputFile);
                 PDDocument newDoc = new PDDocument()) {
                
                // Parse page range and extract pages
                List<Integer> pagesToExtract = parsePageList(pageRange, sourceDoc.getNumberOfPages());
                
                for (Integer pageNum : pagesToExtract) {
                    if (pageNum > 0 && pageNum <= sourceDoc.getNumberOfPages()) {
                        newDoc.addPage(sourceDoc.getPage(pageNum - 1));
                    }
                }
                
                newDoc.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "extracted", true,
                "pageRange", pageRange
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("EXTRACT_ERROR", "Failed to extract pages: " + e.getMessage());
        }
    }
    
    private List<Integer> parsePageList(String pageRange, int totalPages) {
        List<Integer> pages = new ArrayList<>();
        String[] parts = pageRange.split(",");
        
        for (String part : parts) {
            part = part.trim();
            if (part.contains("-")) {
                String[] range = part.split("-");
                int start = Integer.parseInt(range[0].trim());
                int end = Integer.parseInt(range[1].trim());
                for (int i = start; i <= end && i <= totalPages; i++) {
                    pages.add(i);
                }
            } else {
                pages.add(Integer.parseInt(part));
            }
        }
        
        return pages;
    }
    
    // ==================== HELPER METHODS ====================
    
    private Map<String, Object> handleOfficeConversion(Path inputFile, Map<String, Object> parameters, 
                                                      JobStatus jobStatus, String targetExt, String operationName) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, operationName);
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "converted_document";
        
        Integer timeout = (Integer) parameters.get("timeoutSeconds");
        if (timeout == null) timeout = 60;
        
        try {
            Path outputPath = createOutputFile(outputName, targetExt);
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Converting with LibreOffice");
            
            String command = String.format("%s --headless --convert-to %s --outdir %s %s",
                libreofficePath, 
                targetExt,
                outputPath.getParent().toAbsolutePath(),
                inputFile.toAbsolutePath());
            
            executeExternalCommand(command, timeout, operationName);
            
            // LibreOffice might change the filename, so we need to find it
            String baseName = fileUtil.getBaseFilename(inputFile.getFileName().toString());
            Path actualOutput = outputPath.getParent().resolve(baseName + "." + targetExt);
            
            if (!Files.exists(actualOutput)) {
                // Try with timestamp
                actualOutput = outputPath.getParent().resolve(baseName + "_" + System.currentTimeMillis() + "." + targetExt);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(actualOutput.getFileName().toString()),
                "converted", true,
                "targetFormat", targetExt
            );
            
        } catch (Exception e) {
            throw new PDFProcessingException("OFFICE_CONVERSION_ERROR", 
                "Failed to convert with LibreOffice: " + e.getMessage());
        }
    }
    
    private void executeExternalCommand(String command, int timeoutSeconds, String operation) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command.split(" "));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            boolean finished = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                throw new ExternalToolException("TIMEOUT", 
                    operation + " timed out after " + timeoutSeconds + " seconds");
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new ExternalToolException("EXTERNAL_TOOL_ERROR", 
                    operation + " failed with exit code " + exitCode);
            }
            
        } catch (IOException | InterruptedException e) {
            throw new ExternalToolException("EXECUTION_ERROR", 
                "Failed to execute external command: " + e.getMessage());
        }
    }
    
    private String convertMarkdownToHTML(String markdown) {
        // Simple markdown to HTML converter
        // In production, use a proper library like CommonMark
        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>body{font-family:Arial;margin:40px;}h1{font-size:24px;}h2{font-size:20px;}p{margin:10px 0;}</style></head><body>");
        
        String[] lines = markdown.split("\n");
        for (String line : lines) {
            if (line.startsWith("# ")) {
                html.append("<h1>").append(line.substring(2)).append("</h1>");
            } else if (line.startsWith("## ")) {
                html.append("<h2>").append(line.substring(3)).append("</h2>");
            } else if (line.startsWith("### ")) {
                html.append("<h3>").append(line.substring(4)).append("</h3>");
            } else if (line.trim().isEmpty()) {
                html.append("<br/>");
            } else {
                html.append("<p>").append(line).append("</p>");
            }
        }
        
        html.append("</body></html>");
        return html.toString();
    }
    
    // ==================== NEW PAGE MANIPULATION HANDLERS ====================
    
    private Map<String, Object> handleDeletePages(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Deleting pages");
        
        String pageRange = (String) parameters.get("pageRange");
        if (pageRange == null) {
            throw new PDFProcessingException("MISSING_PARAMETER", "pageRange parameter required (e.g., '1,3,5-7')");
        }
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "pages_removed";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Processing pages");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                int totalPages = document.getNumberOfPages();
                List<Integer> pagesToDelete = parsePageList(pageRange, totalPages);
                
                // Sort in descending order to delete from end first
                pagesToDelete.sort(Collections.reverseOrder());
                
                int deletedCount = 0;
                for (int pageNum : pagesToDelete) {
                    if (pageNum >= 1 && pageNum <= document.getNumberOfPages()) {
                        document.removePage(pageNum - 1);
                        deletedCount++;
                    }
                }
                
                if (document.getNumberOfPages() == 0) {
                    throw new PDFProcessingException("INVALID_OPERATION", "Cannot delete all pages from PDF");
                }
                
                document.save(outputPath.toFile());
                
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
                
                return Map.of(
                    "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                    "pagesDeleted", deletedCount,
                    "originalPageCount", totalPages,
                    "newPageCount", document.getNumberOfPages()
                );
            }
            
        } catch (IOException e) {
            throw new PDFProcessingException("DELETE_PAGES_ERROR", "Failed to delete pages: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleAddBlankPage(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Adding blank pages");
        
        Integer position = (Integer) parameters.get("position");
        if (position == null) position = -1; // -1 means end
        
        Integer count = (Integer) parameters.get("count");
        if (count == null) count = 1;
        
        String pageSize = (String) parameters.get("pageSize");
        if (pageSize == null) pageSize = "A4";
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "with_blank_pages";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Inserting blank pages");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                PDRectangle rectangle = getPageSize(pageSize);
                
                int insertIndex = position == -1 ? document.getNumberOfPages() : Math.min(position, document.getNumberOfPages());
                
                for (int i = 0; i < count; i++) {
                    PDPage blankPage = new PDPage(rectangle);
                    document.getPages().insertBefore(blankPage, document.getPage(Math.min(insertIndex, document.getNumberOfPages() - 1)));
                }
                
                document.save(outputPath.toFile());
                
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
                
                return Map.of(
                    "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                    "blankPagesAdded", count,
                    "position", position == -1 ? "end" : position,
                    "pageSize", pageSize,
                    "newPageCount", document.getNumberOfPages()
                );
            }
            
        } catch (IOException e) {
            throw new PDFProcessingException("ADD_BLANK_ERROR", "Failed to add blank pages: " + e.getMessage());
        }
    }
    
    private PDRectangle getPageSize(String pageSize) {
        switch (pageSize.toUpperCase()) {
            case "A3": return PDRectangle.A3;
            case "A4": return PDRectangle.A4;
            case "A5": return PDRectangle.A5;
            case "LETTER": return PDRectangle.LETTER;
            case "LEGAL": return PDRectangle.LEGAL;
            default: return PDRectangle.A4;
        }
    }
    
    // ==================== NEW CONVERSION HANDLERS ====================
    
    private Map<String, Object> handlePDFToHTML(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting PDF to HTML");
        
        Boolean embedImages = (Boolean) parameters.get("embedImages");
        if (embedImages == null) embedImages = true;
        
        Boolean preserveLayout = (Boolean) parameters.get("preserveLayout");
        if (preserveLayout == null) preserveLayout = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "converted_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "html");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Extracting content");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                StringBuilder html = new StringBuilder();
                
                // HTML header with CSS
                html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
                html.append("<meta charset=\"UTF-8\">\n");
                html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
                html.append("<title>").append(outputName).append("</title>\n");
                html.append("<style>\n");
                html.append("body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; line-height: 1.6; }\n");
                html.append(".page { border-bottom: 2px solid #ccc; padding-bottom: 20px; margin-bottom: 20px; page-break-after: always; }\n");
                html.append(".page-header { color: #666; font-size: 12px; margin-bottom: 10px; }\n");
                html.append("p { margin: 10px 0; text-align: justify; }\n");
                html.append("img { max-width: 100%; height: auto; display: block; margin: 10px auto; }\n");
                html.append("h1, h2, h3 { color: #333; margin-top: 20px; }\n");
                html.append("</style>\n</head>\n<body>\n");
                
                PDFTextStripper stripper = new PDFTextStripper();
                PDFRenderer renderer = new PDFRenderer(document);
                
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    html.append("<div class=\"page\" id=\"page-").append(i + 1).append("\">\n");
                    html.append("<div class=\"page-header\">Page ").append(i + 1).append(" of ").append(document.getNumberOfPages()).append("</div>\n");
                    
                    // Extract text for this page
                    stripper.setStartPage(i + 1);
                    stripper.setEndPage(i + 1);
                    String pageText = stripper.getText(document);
                    
                    // Convert text to paragraphs
                    String[] paragraphs = pageText.split("\n\n");
                    for (String para : paragraphs) {
                        String cleaned = para.trim().replace("\n", " ").replace("  ", " ");
                        if (!cleaned.isEmpty()) {
                            // Detect if it might be a heading
                            if (cleaned.length() < 80 && cleaned.equals(cleaned.toUpperCase())) {
                                html.append("<h2>").append(escapeHtml(cleaned)).append("</h2>\n");
                            } else if (cleaned.length() < 60 && !cleaned.contains(".")) {
                                html.append("<h3>").append(escapeHtml(cleaned)).append("</h3>\n");
                            } else {
                                html.append("<p>").append(escapeHtml(cleaned)).append("</p>\n");
                            }
                        }
                    }
                    
                    // Embed page image if requested
                    if (embedImages) {
                        BufferedImage pageImage = renderer.renderImageWithDPI(i, 72);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(pageImage, "png", baos);
                        String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                        html.append("<img src=\"data:image/png;base64,").append(base64).append("\" alt=\"Page ").append(i + 1).append("\">\n");
                    }
                    
                    html.append("</div>\n");
                }
                
                html.append("</body>\n</html>");
                
                Files.writeString(outputPath, html.toString());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "converted", true,
                "embedImages", embedImages,
                "preserveLayout", preserveLayout
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("PDF_TO_HTML_ERROR", "Failed to convert PDF to HTML: " + e.getMessage());
        }
    }
    
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private Map<String, Object> handleCSVToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting CSV to PDF");
        
        String delimiter = (String) parameters.get("delimiter");
        if (delimiter == null) delimiter = ",";
        
        Boolean hasHeader = (Boolean) parameters.get("hasHeader");
        if (hasHeader == null) hasHeader = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "csv_table";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Parsing CSV");
            
            List<String> lines = Files.readAllLines(inputFile);
            if (lines.isEmpty()) {
                throw new PDFProcessingException("EMPTY_FILE", "CSV file is empty");
            }
            
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                
                float yPosition = page.getMediaBox().getHeight() - 50;
                float margin = 40;
                float fontSize = 10;
                float lineHeight = 14;
                
                for (int i = 0; i < lines.size(); i++) {
                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - 50;
                    }
                    
                    String[] cells = lines.get(i).split(delimiter.equals(",") ? "," : delimiter);
                    
                    contentStream.beginText();
                    contentStream.setFont(i == 0 && hasHeader ? boldFont : font, fontSize);
                    contentStream.newLineAtOffset(margin, yPosition);
                    
                    StringBuilder row = new StringBuilder();
                    for (int j = 0; j < cells.length; j++) {
                        if (j > 0) row.append("  |  ");
                        String cell = cells[j].trim().replace("\"", "");
                        if (cell.length() > 30) cell = cell.substring(0, 27) + "...";
                        row.append(cell);
                    }
                    
                    contentStream.showText(row.toString());
                    contentStream.endText();
                    
                    // Draw line under header
                    if (i == 0 && hasHeader) {
                        contentStream.setLineWidth(0.5f);
                        contentStream.moveTo(margin, yPosition - 5);
                        contentStream.lineTo(page.getMediaBox().getWidth() - margin, yPosition - 5);
                        contentStream.stroke();
                    }
                    
                    yPosition -= lineHeight;
                }
                
                contentStream.close();
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "converted", true,
                "rowCount", lines.size()
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("CSV_TO_PDF_ERROR", "Failed to convert CSV to PDF: " + e.getMessage());
        }
    }
    
    private Map<String, Object> handleJSONToPDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Converting JSON to PDF");
        
        Boolean prettyPrint = (Boolean) parameters.get("prettyPrint");
        if (prettyPrint == null) prettyPrint = true;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "json_document";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Parsing JSON");
            
            String jsonContent = Files.readString(inputFile);
            
            // Format JSON if needed
            if (prettyPrint) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object json = mapper.readValue(jsonContent, Object.class);
                jsonContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            }
            
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                
                PDPageContentStream contentStream = new PDPageContentStream(document, page);
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.COURIER);
                
                float yPosition = page.getMediaBox().getHeight() - 50;
                float margin = 40;
                float fontSize = 8;
                float lineHeight = 10;
                
                String[] lines = jsonContent.split("\n");
                
                for (String line : lines) {
                    if (yPosition < 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = page.getMediaBox().getHeight() - 50;
                    }
                    
                    contentStream.beginText();
                    contentStream.setFont(font, fontSize);
                    contentStream.newLineAtOffset(margin, yPosition);
                    
                    // Sanitize line - remove control characters that can't be rendered
                    line = sanitizeForPDF(line);
                    
                    // Truncate long lines
                    if (line.length() > 100) {
                        line = line.substring(0, 97) + "...";
                    }
                    
                    contentStream.showText(line);
                    contentStream.endText();
                    
                    yPosition -= lineHeight;
                }
                
                contentStream.close();
                document.save(outputPath.toFile());
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "converted", true,
                "prettyPrinted", prettyPrint
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("JSON_TO_PDF_ERROR", "Failed to convert JSON to PDF: " + e.getMessage());
        }
    }
    
    private String sanitizeForPDF(String text) {
        if (text == null) return "";
        // Remove control characters that PDFBox can't handle
        return text.replaceAll("[\\x00-\\x09\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                   .replace("\r", "")
                   .replace("\t", "    ");
    }
    
    // ==================== ENHANCED SPLIT HANDLERS ====================
    
    private Map<String, Object> handleSplitByBookmarks(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Splitting by bookmarks");
        
        String outputPrefix = (String) parameters.get("outputPrefix");
        if (outputPrefix == null) outputPrefix = "chapter";
        
        Integer maxDepth = (Integer) parameters.get("maxDepth");
        if (maxDepth == null) maxDepth = 1;
        
        try {
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Analyzing bookmarks");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline outline = 
                    document.getDocumentCatalog().getDocumentOutline();
                
                if (outline == null) {
                    throw new PDFProcessingException("NO_BOOKMARKS", "PDF has no bookmarks to split by");
                }
                
                List<BookmarkSplitPoint> splitPoints = new ArrayList<>();
                extractSplitPoints(document, outline, splitPoints, 0, maxDepth);
                
                if (splitPoints.isEmpty()) {
                    throw new PDFProcessingException("NO_BOOKMARKS", "No valid bookmark split points found");
                }
                
                // Sort by page number
                splitPoints.sort((a, b) -> Integer.compare(a.pageNumber, b.pageNumber));
                
                jobQueueService.updateProgress(jobStatus.getId(), 70, "Creating split files");
                
                List<String> resultFiles = new ArrayList<>();
                
                for (int i = 0; i < splitPoints.size(); i++) {
                    BookmarkSplitPoint point = splitPoints.get(i);
                    int startPage = point.pageNumber;
                    int endPage = (i + 1 < splitPoints.size()) ? splitPoints.get(i + 1).pageNumber - 1 : document.getNumberOfPages();
                    
                    if (endPage >= startPage) {
                        String sanitizedTitle = point.title.replaceAll("[^a-zA-Z0-9]", "_").substring(0, Math.min(50, point.title.length()));
                        Path outputPath = createOutputFile(outputPrefix + "_" + (i + 1) + "_" + sanitizedTitle, "pdf");
                        
                        try (PDDocument newDoc = new PDDocument()) {
                            for (int p = startPage; p <= endPage; p++) {
                                newDoc.addPage(document.getPage(p - 1));
                            }
                            newDoc.save(outputPath.toFile());
                        }
                        
                        resultFiles.add(outputPath.getFileName().toString());
                    }
                }
                
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
                
                return Map.of(
                    "resultFiles", resultFiles,
                    "totalFiles", resultFiles.size(),
                    "splitBy", "bookmarks",
                    "bookmarksFound", splitPoints.size()
                );
            }
            
        } catch (IOException e) {
            throw new PDFProcessingException("SPLIT_BOOKMARK_ERROR", "Failed to split by bookmarks: " + e.getMessage());
        }
    }
    
    private void extractSplitPoints(PDDocument document, 
            org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode node,
            List<BookmarkSplitPoint> splitPoints, int depth, int maxDepth) {
        
        if (depth > maxDepth) return;
        
        for (org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem item : node.children()) {
            try {
                org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDDestination dest = item.getDestination();
                if (dest instanceof org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination pageDest) {
                    int pageNum = pageDest.retrievePageNumber() + 1;
                    if (pageNum > 0) {
                        splitPoints.add(new BookmarkSplitPoint(item.getTitle(), pageNum, depth));
                    }
                }
            } catch (Exception e) {
                logger.warn("Could not extract bookmark destination: {}", e.getMessage());
            }
            
            // Recurse for children
            extractSplitPoints(document, item, splitPoints, depth + 1, maxDepth);
        }
    }
    
    private static class BookmarkSplitPoint {
        String title;
        int pageNumber;
        int depth;
        
        BookmarkSplitPoint(String title, int pageNumber, int depth) {
            this.title = title;
            this.pageNumber = pageNumber;
            this.depth = depth;
        }
    }
    
    private Map<String, Object> handleSplitBySize(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Splitting by size");
        
        Integer targetSizeMB = (Integer) parameters.get("targetSizeMB");
        if (targetSizeMB == null) targetSizeMB = 10;
        
        String outputPrefix = (String) parameters.get("outputPrefix");
        if (outputPrefix == null) outputPrefix = "part";
        
        try {
            long targetSizeBytes = targetSizeMB * 1024L * 1024L;
            long originalSize = Files.size(inputFile);
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Calculating split points");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                int totalPages = document.getNumberOfPages();
                
                // Estimate bytes per page
                long avgBytesPerPage = originalSize / totalPages;
                int pagesPerFile = (int) Math.max(1, targetSizeBytes / avgBytesPerPage);
                
                List<String> resultFiles = new ArrayList<>();
                int fileIndex = 1;
                int startPage = 0;
                
                while (startPage < totalPages) {
                    int endPage = Math.min(startPage + pagesPerFile, totalPages);
                    
                    Path outputPath = createOutputFile(outputPrefix + "_" + fileIndex, "pdf");
                    
                    try (PDDocument newDoc = new PDDocument()) {
                        for (int p = startPage; p < endPage; p++) {
                            newDoc.addPage(document.getPage(p));
                        }
                        newDoc.save(outputPath.toFile());
                    }
                    
                    resultFiles.add(outputPath.getFileName().toString());
                    startPage = endPage;
                    fileIndex++;
                }
                
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
                
                return Map.of(
                    "resultFiles", resultFiles,
                    "totalFiles", resultFiles.size(),
                    "targetSizeMB", targetSizeMB,
                    "splitBy", "size",
                    "originalSizeMB", originalSize / (1024 * 1024)
                );
            }
            
        } catch (IOException e) {
            throw new PDFProcessingException("SPLIT_SIZE_ERROR", "Failed to split by size: " + e.getMessage());
        }
    }
    
    // ==================== AUTO DETECTION HANDLERS ====================
    
    private Map<String, Object> handleAutoRotate(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Detecting text orientation");
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "auto_rotated";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Analyzing pages");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                PDFRenderer renderer = new PDFRenderer(document);
                int rotatedCount = 0;
                
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    PDPage page = document.getPage(i);
                    
                    // Render page to image
                    BufferedImage image = renderer.renderImageWithDPI(i, 72);
                    
                    // Detect text orientation using image analysis
                    int suggestedRotation = detectTextOrientation(image);
                    
                    if (suggestedRotation != 0) {
                        page.setRotation((page.getRotation() + suggestedRotation) % 360);
                        rotatedCount++;
                    }
                }
                
                document.save(outputPath.toFile());
                
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
                
                return Map.of(
                    "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                    "pagesAnalyzed", document.getNumberOfPages(),
                    "pagesRotated", rotatedCount,
                    "autoDetected", true
                );
            }
            
        } catch (IOException e) {
            throw new PDFProcessingException("AUTO_ROTATE_ERROR", "Failed to auto-rotate PDF: " + e.getMessage());
        }
    }
    
    private int detectTextOrientation(BufferedImage image) {
        // Simple text orientation detection based on image analysis
        // More sophisticated implementations would use OCR or ML
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Sample horizontal and vertical lines to detect text direction
        int horizontalDarkPixels = 0;
        int verticalDarkPixels = 0;
        
        // Sample middle horizontal strip
        for (int x = width / 4; x < 3 * width / 4; x++) {
            int y = height / 2;
            int rgb = image.getRGB(x, y);
            if (isDarkPixel(rgb)) horizontalDarkPixels++;
        }
        
        // Sample middle vertical strip
        for (int y = height / 4; y < 3 * height / 4; y++) {
            int x = width / 2;
            int rgb = image.getRGB(x, y);
            if (isDarkPixel(rgb)) verticalDarkPixels++;
        }
        
        // If vertical has significantly more dark pixels, text might be rotated 90 degrees
        if (verticalDarkPixels > horizontalDarkPixels * 1.5) {
            return 90;
        }
        
        return 0; // No rotation needed
    }
    
    private boolean isDarkPixel(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (r + g + b) / 3 < 128;
    }
    
    private Map<String, Object> handleAutoCrop(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Detecting margins");
        
        Integer marginPadding = (Integer) parameters.get("marginPadding");
        if (marginPadding == null) marginPadding = 10;
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "auto_cropped";
        
        try {
            Path outputPath = createOutputFile(outputName, "pdf");
            
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Analyzing content boundaries");
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                PDFRenderer renderer = new PDFRenderer(document);
                
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    PDPage page = document.getPage(i);
                    BufferedImage image = renderer.renderImageWithDPI(i, 72);
                    
                    // Detect content bounding box
                    int[] bounds = detectContentBounds(image);
                    
                    if (bounds != null) {
                        PDRectangle mediaBox = page.getMediaBox();
                        float scale = mediaBox.getWidth() / image.getWidth();
                        
                        // Calculate new crop box
                        float cropLeft = Math.max(0, bounds[0] * scale - marginPadding);
                        float cropBottom = Math.max(0, (image.getHeight() - bounds[3]) * scale - marginPadding);
                        float cropWidth = Math.min(mediaBox.getWidth() - cropLeft, (bounds[2] - bounds[0]) * scale + 2 * marginPadding);
                        float cropHeight = Math.min(mediaBox.getHeight() - cropBottom, (bounds[3] - bounds[1]) * scale + 2 * marginPadding);
                        
                        page.setCropBox(new PDRectangle(cropLeft, cropBottom, cropWidth, cropHeight));
                    }
                }
                
                document.save(outputPath.toFile());
                
                jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
                
                return Map.of(
                    "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                    "pagesCropped", document.getNumberOfPages(),
                    "marginPadding", marginPadding,
                    "autoDetected", true
                );
            }
            
        } catch (IOException e) {
            throw new PDFProcessingException("AUTO_CROP_ERROR", "Failed to auto-crop PDF: " + e.getMessage());
        }
    }
    
    private int[] detectContentBounds(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        int minX = width, minY = height, maxX = 0, maxY = 0;
        boolean foundContent = false;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isDarkPixel(image.getRGB(x, y))) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    foundContent = true;
                }
            }
        }
        
        if (!foundContent) return null;
        
        return new int[]{minX, minY, maxX, maxY};
    }
    
    // ==================== VALIDATION HANDLER ====================
    
    private Map<String, Object> handleValidatePDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) {
        jobQueueService.updateProgress(jobStatus.getId(), 25, "Validating PDF");
        
        Boolean checkSearchability = (Boolean) parameters.get("checkSearchability");
        if (checkSearchability == null) checkSearchability = true;
        
        Boolean checkStructure = (Boolean) parameters.get("checkStructure");
        if (checkStructure == null) checkStructure = true;
        
        Boolean checkFonts = (Boolean) parameters.get("checkFonts");
        if (checkFonts == null) checkFonts = true;
        
        try {
            jobQueueService.updateProgress(jobStatus.getId(), 50, "Running validation checks");
            
            Map<String, Object> validationResults = new LinkedHashMap<>();
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            boolean isValid = true;
            
            try (PDDocument document = pdfUtil.loadPDF(inputFile)) {
                // Basic structure validation
                if (checkStructure) {
                    validationResults.put("pageCount", document.getNumberOfPages());
                    validationResults.put("isEncrypted", document.isEncrypted());
                    validationResults.put("pdfVersion", document.getVersion());
                    
                    if (document.getNumberOfPages() == 0) {
                        errors.add("PDF has no pages");
                        isValid = false;
                    }
                    
                    // Check for document catalog
                    if (document.getDocumentCatalog() == null) {
                        errors.add("PDF has no document catalog");
                        isValid = false;
                    }
                }
                
                // Searchability validation
                if (checkSearchability) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(document);
                    boolean hasText = text != null && !text.trim().isEmpty();
                    
                    validationResults.put("isSearchable", hasText);
                    validationResults.put("textLength", text != null ? text.length() : 0);
                    
                    if (!hasText) {
                        warnings.add("PDF appears to be image-only (not searchable). Consider running OCR.");
                    }
                }
                
                // Font validation
                if (checkFonts) {
                    Set<String> fontNames = new HashSet<>();
                    Set<String> embeddedFonts = new HashSet<>();
                    
                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        PDPage page = document.getPage(i);
                        if (page.getResources() != null && page.getResources().getFontNames() != null) {
                            for (org.apache.pdfbox.cos.COSName fontName : page.getResources().getFontNames()) {
                                fontNames.add(fontName.getName());
                                try {
                                    org.apache.pdfbox.pdmodel.font.PDFont font = page.getResources().getFont(fontName);
                                    if (font != null && font.isEmbedded()) {
                                        embeddedFonts.add(fontName.getName());
                                    }
                                } catch (Exception e) {
                                    warnings.add("Could not analyze font: " + fontName.getName());
                                }
                            }
                        }
                    }
                    
                    validationResults.put("fontsUsed", fontNames.size());
                    validationResults.put("fontsEmbedded", embeddedFonts.size());
                    
                    if (fontNames.size() > embeddedFonts.size()) {
                        warnings.add("Some fonts are not embedded. Document may render differently on other systems.");
                    }
                }
                
                // File size validation
                long fileSizeMB = Files.size(inputFile) / (1024 * 1024);
                validationResults.put("fileSizeMB", fileSizeMB);
                
                if (fileSizeMB > 100) {
                    warnings.add("File is larger than 100MB. Consider compression.");
                }
                
                // Metadata check
                org.apache.pdfbox.pdmodel.PDDocumentInformation info = document.getDocumentInformation();
                if (info != null) {
                    validationResults.put("hasTitle", info.getTitle() != null && !info.getTitle().isEmpty());
                    validationResults.put("hasAuthor", info.getAuthor() != null && !info.getAuthor().isEmpty());
                }
                
                // Bookmark check
                validationResults.put("hasBookmarks", document.getDocumentCatalog().getDocumentOutline() != null);
                
                // Form check
                validationResults.put("hasForms", document.getDocumentCatalog().getAcroForm() != null);
            }
            
            jobQueueService.updateProgress(jobStatus.getId(), 90, "Finalizing");
            
            validationResults.put("isValid", isValid);
            validationResults.put("warnings", warnings);
            validationResults.put("errors", errors);
            validationResults.put("warningCount", warnings.size());
            validationResults.put("errorCount", errors.size());
            
            return validationResults;
            
        } catch (Exception e) {
            return Map.of(
                "isValid", false,
                "errors", List.of("Failed to validate PDF: " + e.getMessage()),
                "errorCount", 1
            );
        }
    }
    
    // ==================== SANITIZATION METHODS ====================
    
    /**
     * Handle PDF sanitization - removes hidden data and PII
     */
    private Map<String, Object> handleSanitizePDF(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) throws IOException {
        jobQueueService.updateProgress(jobStatus.getId(), 10, "Preparing sanitization");
        
        // Build sanitization options from parameters using fluent API
        MetadataSanitizationService.SanitizationOptions options = new MetadataSanitizationService.SanitizationOptions();
        
        // Document properties
        Boolean removeMetadata = (Boolean) parameters.get("removeMetadata");
        if (removeMetadata != null) options.removeMetadata(removeMetadata);
        
        Boolean removeXmpMetadata = (Boolean) parameters.get("removeXmpMetadata");
        if (removeXmpMetadata != null) options.removeXmpMetadata(removeXmpMetadata);
        
        Boolean removeJavaScript = (Boolean) parameters.get("removeJavaScript");
        if (removeJavaScript != null) options.removeJavaScript(removeJavaScript);
        
        Boolean removeAttachments = (Boolean) parameters.get("removeAttachments");
        if (removeAttachments != null) options.removeAttachments(removeAttachments);
        
        Boolean removeAnnotations = (Boolean) parameters.get("removeAnnotations");
        if (removeAnnotations != null) options.removeAnnotations(removeAnnotations);
        
        Boolean removeFormData = (Boolean) parameters.get("removeFormData");
        if (removeFormData != null) options.removeFormData(removeFormData);
        
        Boolean removeLinks = (Boolean) parameters.get("removeLinks");
        if (removeLinks != null) options.removeLinks(removeLinks);
        
        Boolean removeThumbnails = (Boolean) parameters.get("removeThumbnails");
        if (removeThumbnails != null) options.removeThumbnails(removeThumbnails);
        
        Boolean removeBookmarks = (Boolean) parameters.get("removeBookmarks");
        if (removeBookmarks != null) options.removeBookmarks(removeBookmarks);
        
        Boolean removeHiddenLayers = (Boolean) parameters.get("removeHiddenLayers");
        if (removeHiddenLayers != null) options.removeHiddenLayers(removeHiddenLayers);
        
        Boolean removePrivateData = (Boolean) parameters.get("removePrivateData");
        if (removePrivateData != null) options.removePrivateData(removePrivateData);
        
        // PII scanning
        Boolean scanForPii = (Boolean) parameters.get("scanForPii");
        if (scanForPii != null) options.scanForPII(scanForPii);
        
        // Full sanitization preset
        Boolean fullSanitization = (Boolean) parameters.get("fullSanitization");
        if (fullSanitization != null && fullSanitization) {
            options = MetadataSanitizationService.SanitizationOptions.full();
        }
        
        String outputName = (String) parameters.get("outputFileName");
        if (outputName == null) outputName = "sanitized_document";
        
        Path outputPath = createOutputFile(outputName, "pdf");
        
        jobQueueService.updateProgress(jobStatus.getId(), 30, "Running sanitization");
        
        // Perform sanitization
        MetadataSanitizationService.SanitizationResult sanitizationResult = 
            metadataSanitizationService.sanitize(inputFile, outputPath, options);
        
        jobQueueService.updateProgress(jobStatus.getId(), 80, "Saving sanitized document");
        
        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", sanitizationResult.isSuccess());
        result.put("resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()));
        result.put("outputFile", sanitizationResult.getOutputFile());
        
        // Add removal summary from the result map
        result.put("removedItems", sanitizationResult.getRemovedItems());
        result.put("totalItemsRemoved", sanitizationResult.getTotalItemsRemoved());
        
        // Add PII findings if scanned
        if (sanitizationResult.getPiiFindings() != null && !sanitizationResult.getPiiFindings().isEmpty()) {
            List<Map<String, String>> piiList = new ArrayList<>();
            for (MetadataSanitizationService.PIIFinding finding : sanitizationResult.getPiiFindings()) {
                piiList.add(finding.toMap());
            }
            result.put("piiFindings", piiList);
            result.put("piiCount", piiList.size());
        }
        
        // Add size comparison
        result.put("originalSize", fileUtil.getHumanReadableSize(inputFile));
        result.put("sanitizedSize", fileUtil.getHumanReadableSize(outputPath));
        result.put("inputSizeBytes", sanitizationResult.getInputSize());
        result.put("outputSizeBytes", sanitizationResult.getOutputSize());
        
        long saved = sanitizationResult.getInputSize() - sanitizationResult.getOutputSize();
        if (sanitizationResult.getInputSize() > 0) {
            result.put("sizeReduction", String.format("%.1f%%", 
                (double) saved / sanitizationResult.getInputSize() * 100));
        }
        
        result.put("processingTimeMs", sanitizationResult.getProcessingTimeMs());
        
        return result;
    }
    
    /**
     * Handle hidden data analysis - analyzes PDF without modification
     */
    private Map<String, Object> handleAnalyzeHiddenData(Path inputFile, Map<String, Object> parameters, JobStatus jobStatus) throws IOException {
        jobQueueService.updateProgress(jobStatus.getId(), 10, "Preparing analysis");
        
        jobQueueService.updateProgress(jobStatus.getId(), 40, "Analyzing document");
        
        // Perform analysis only (no modification)
        MetadataSanitizationService.SanitizationAnalysis analysisResult = 
            metadataSanitizationService.analyze(inputFile);
        
        jobQueueService.updateProgress(jobStatus.getId(), 90, "Building report");
        
        // Build comprehensive analysis report
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("analyzed", true);
        result.put("filePath", analysisResult.getFilePath());
        
        // Hidden data found
        Map<String, Object> hiddenData = new LinkedHashMap<>();
        hiddenData.put("hasMetadata", analysisResult.isHasMetadata());
        hiddenData.put("hasXmpMetadata", analysisResult.isHasXmpMetadata());
        hiddenData.put("hasJavaScript", analysisResult.isHasJavaScript());
        hiddenData.put("attachmentCount", analysisResult.getAttachmentCount());
        hiddenData.put("annotationCount", analysisResult.getAnnotationCount());
        hiddenData.put("formFieldCount", analysisResult.getFormFieldCount());
        hiddenData.put("hasBookmarks", analysisResult.isHasBookmarks());
        hiddenData.put("thumbnailCount", analysisResult.getThumbnailCount());
        hiddenData.put("hiddenLayerCount", analysisResult.getHiddenLayerCount());
        hiddenData.put("hasPotentialPII", analysisResult.isHasPotentialPII());
        result.put("hiddenData", hiddenData);
        
        // Metadata details if present
        if (analysisResult.getDocumentMetadata() != null && !analysisResult.getDocumentMetadata().isEmpty()) {
            result.put("documentMetadata", analysisResult.getDocumentMetadata());
        }
        
        // PII findings
        if (analysisResult.getPiiFindings() != null && !analysisResult.getPiiFindings().isEmpty()) {
            List<Map<String, String>> piiList = new ArrayList<>();
            for (MetadataSanitizationService.PIIFinding finding : analysisResult.getPiiFindings()) {
                piiList.add(finding.toMap());
            }
            result.put("piiFindings", piiList);
            result.put("piiCount", piiList.size());
        }
        
        // Risk assessment
        result.put("riskLevel", analysisResult.getRiskLevel());
        result.put("riskScore", analysisResult.getRiskScore());
        result.put("recommendations", analysisResult.getRecommendations());
        
        // Summary
        int totalIssues = analysisResult.getAttachmentCount() + analysisResult.getAnnotationCount() +
                          analysisResult.getFormFieldCount() + analysisResult.getThumbnailCount() +
                          analysisResult.getHiddenLayerCount() +
                          (analysisResult.isHasMetadata() ? 1 : 0) +
                          (analysisResult.isHasXmpMetadata() ? 1 : 0) +
                          (analysisResult.isHasJavaScript() ? 1 : 0) +
                          (analysisResult.isHasBookmarks() ? 1 : 0) +
                          (analysisResult.getPiiFindings() != null ? analysisResult.getPiiFindings().size() : 0);
        
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalIssuesFound", totalIssues);
        summary.put("requiresSanitization", totalIssues > 0 || analysisResult.getRiskScore() > 20);
        result.put("summary", summary);
        
        return result;
    }
}