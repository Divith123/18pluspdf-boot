package com.chnindia.eighteenpluspdf.util;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

/**
 * Utility class for PDF operations using Apache PDFBox 3.x
 * Handles all core PDF manipulation tasks including loading, merging, splitting,
 * rotating, watermarking, encryption, and metadata operations.
 */
@Component
public class PDFUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PDFUtil.class);
    
    @Value("${app.pdf.max-pages:2000}")
    private int maxPages;
    
    @Value("${app.pdf.max-file-size-mb:500}")
    private int maxFileSizeMB;
    
    /**
     * Load PDF document with validation (PDFBox 3.x API)
     */
    public PDDocument loadPDF(Path filePath) throws IOException {
        validatePDFFile(filePath);
        return Loader.loadPDF(filePath.toFile());
    }
    
    /**
     * Load PDF from input stream (PDFBox 3.x API)
     */
    public PDDocument loadPDF(InputStream inputStream) throws IOException {
        return Loader.loadPDF(new RandomAccessReadBuffer(inputStream));
    }
    
    /**
     * Load PDF with password (PDFBox 3.x API)
     */
    public PDDocument loadPDF(Path filePath, String password) throws IOException {
        validatePDFFile(filePath);
        return Loader.loadPDF(filePath.toFile(), password);
    }
    
    /**
     * Extract text from PDF
     */
    public String extractText(Path filePath) throws IOException {
        try (PDDocument document = loadPDF(filePath)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Extract text from specific page range
     */
    public String extractText(Path filePath, String pageRange) throws IOException {
        try (PDDocument document = loadPDF(filePath)) {
            PDFTextStripper stripper = new PDFTextStripper();
            
            if (!"all".equals(pageRange)) {
                PageRange range = parsePageRange(pageRange, document.getNumberOfPages());
                stripper.setStartPage(range.start);
                stripper.setEndPage(range.end);
            }
            
            return stripper.getText(document);
        }
    }
    
    /**
     * Get page count
     */
    public int getPageCount(Path filePath) throws IOException {
        try (PDDocument document = loadPDF(filePath)) {
            return document.getNumberOfPages();
        }
    }
    
    /**
     * Render PDF pages as images
     */
    public List<BufferedImage> renderPages(Path filePath, int dpi) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = loadPDF(filePath)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                images.add(image);
            }
        }
        return images;
    }
    
    /**
     * Render specific page range as images
     */
    public List<BufferedImage> renderPages(Path filePath, int dpi, String pageRange) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = loadPDF(filePath)) {
            PDFRenderer renderer = new PDFRenderer(document);
            PageRange range = parsePageRange(pageRange, document.getNumberOfPages());
            
            for (int page = range.start - 1; page < range.end; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, dpi);
                images.add(image);
            }
        }
        return images;
    }
    
    /**
     * Merge multiple PDF files
     */
    public Path mergePDFs(List<Path> inputFiles, Path outputFile, boolean preserveBookmarks) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        
        for (Path file : inputFiles) {
            merger.addSource(file.toFile());
        }
        
        merger.setDestinationFileName(outputFile.toString());
        // PDFBox 3.x requires a MemoryUsageSetting parameter
        merger.mergeDocuments(org.apache.pdfbox.io.IOUtils.createMemoryOnlyStreamCache());
        
        logger.info("Merged {} PDF files into {}", inputFiles.size(), outputFile);
        return outputFile;
    }
    
    /**
     * Split PDF by page ranges or every N pages
     */
    public List<Path> splitPDF(Path inputFile, Path outputDir, String outputPrefix, 
                               int splitEveryNPages, String pageRanges) throws IOException {
        List<Path> outputFiles = new ArrayList<>();
        
        try (PDDocument document = loadPDF(inputFile)) {
            Splitter splitter = new Splitter();
            
            if (pageRanges != null && !pageRanges.isEmpty()) {
                // Split by custom page ranges
                List<PageRange> ranges = parseMultiplePageRanges(pageRanges, document.getNumberOfPages());
                for (int i = 0; i < ranges.size(); i++) {
                    PageRange range = ranges.get(i);
                    splitter.setStartPage(range.start);
                    splitter.setEndPage(range.end);
                    
                    PDDocument splitDoc = splitter.split(document).get(0);
                    Path outputFile = outputDir.resolve(outputPrefix + "_part" + (i + 1) + ".pdf");
                    splitDoc.save(outputFile.toFile());
                    splitDoc.close();
                    outputFiles.add(outputFile);
                }
            } else {
                // Split by every N pages
                splitter.setSplitAtPage(splitEveryNPages);
                List<PDDocument> splitDocs = splitter.split(document);
                
                for (int i = 0; i < splitDocs.size(); i++) {
                    Path outputFile = outputDir.resolve(outputPrefix + "_part" + (i + 1) + ".pdf");
                    splitDocs.get(i).save(outputFile.toFile());
                    splitDocs.get(i).close();
                    outputFiles.add(outputFile);
                }
            }
        }
        
        return outputFiles;
    }
    
    /**
     * Rotate PDF pages
     */
    public Path rotatePDF(Path inputFile, Path outputFile, int angle, String pageRange) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            PDPageTree pages = document.getPages();
            int totalPages = document.getNumberOfPages();
            PageRange range = parsePageRange(pageRange, totalPages);
            
            int pageIndex = 0;
            for (PDPage page : pages) {
                if (pageIndex >= range.start - 1 && pageIndex < range.end) {
                    int currentRotation = page.getRotation();
                    page.setRotation((currentRotation + angle) % 360);
                }
                pageIndex++;
            }
            
            document.save(outputFile.toFile());
            return outputFile;
        }
    }
    
    /**
     * Add watermark to PDF
     */
    public Path addWatermark(Path inputFile, Path outputFile, String text, String fontName, 
                            int fontSize, String color, double opacity, String position, 
                            int rotation, boolean diagonal) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            for (PDPage page : document.getPages()) {
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page, 
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                    
                    // Set transparency
                    PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
                    graphicsState.setNonStrokingAlphaConstant((float) opacity);
                    contentStream.setGraphicsStateParameters(graphicsState);
                    
                    // Set font and color
                    PDType1Font font = getFont(fontName);
                    Color awtColor = Color.decode(color);
                    contentStream.setNonStrokingColor(awtColor);
                    contentStream.setFont(font, fontSize);
                    
                    // Calculate position
                    PDRectangle pageSize = page.getMediaBox();
                    float x = 0, y = 0;
                    
                    if (diagonal) {
                        // Diagonal watermark
                        contentStream.beginText();
                        contentStream.newLineAtOffset(pageSize.getWidth() / 4, pageSize.getHeight() / 2);
                        contentStream.showText(text);
                        contentStream.endText();
                    } else {
                        // Position-based watermark
                        switch (position) {
                            case "center" -> {
                                x = (pageSize.getWidth() - fontSize * text.length()) / 2;
                                y = pageSize.getHeight() / 2;
                            }
                            case "top-left" -> {
                                x = 20;
                                y = pageSize.getHeight() - 20;
                            }
                            case "top-right" -> {
                                x = pageSize.getWidth() - fontSize * text.length() - 20;
                                y = pageSize.getHeight() - 20;
                            }
                            case "bottom-left" -> {
                                x = 20;
                                y = 20 + fontSize;
                            }
                            case "bottom-right" -> {
                                x = pageSize.getWidth() - fontSize * text.length() - 20;
                                y = 20 + fontSize;
                            }
                        }
                        
                        contentStream.beginText();
                        contentStream.newLineAtOffset(x, y);
                        contentStream.showText(text);
                        contentStream.endText();
                    }
                }
            }
            
            document.save(outputFile.toFile());
            return outputFile;
        }
    }
    
    /**
     * Encrypt PDF
     */
    public Path encryptPDF(Path inputFile, Path outputFile, String ownerPassword, String userPassword,
                          boolean allowPrint, boolean allowCopy, boolean allowModify, 
                          boolean allowAnnotate, boolean allowFillForms, boolean allowAccessibility,
                          boolean allowAssemble, boolean allowDegradedPrint) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            AccessPermission accessPermission = new AccessPermission();
            accessPermission.setCanPrint(allowPrint);
            accessPermission.setCanExtractContent(allowCopy);
            accessPermission.setCanModify(allowModify);
            accessPermission.setCanModifyAnnotations(allowAnnotate);
            accessPermission.setCanFillInForm(allowFillForms);
            accessPermission.setCanExtractForAccessibility(allowAccessibility);
            accessPermission.setCanAssembleDocument(allowAssemble);
            // Note: setCanPrintDegraded removed in PDFBox 3.x, use setCanPrint instead
            if (allowDegradedPrint && !allowPrint) {
                logger.info("Degraded print requires full print permission in PDFBox 3.x");
            }
            
            StandardProtectionPolicy policy = new StandardProtectionPolicy(ownerPassword, userPassword, accessPermission);
            policy.setEncryptionKeyLength(256);
            
            document.protect(policy);
            document.save(outputFile.toFile());
            
            return outputFile;
        }
    }
    
    /**
     * Decrypt PDF
     */
    public Path decryptPDF(Path inputFile, Path outputFile, String password) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            if (document.isEncrypted()) {
                document.setAllSecurityToBeRemoved(true);
            }
            document.save(outputFile.toFile());
            return outputFile;
        }
    }
    
    /**
     * Add page numbers
     */
    public Path addPageNumbers(Path inputFile, Path outputFile, String format, String fontName,
                              int fontSize, String color, String position, int margin, 
                              String startPage) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            int totalPages = document.getNumberOfPages();
            PageRange range = parsePageRange(startPage, totalPages);
            
            int pageIndex = 0;
            for (PDPage page : document.getPages()) {
                if (pageIndex >= range.start - 1 && pageIndex < range.end) {
                    try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                        
                        PDType1Font font = getFont(fontName);
                        Color awtColor = Color.decode(color);
                        contentStream.setNonStrokingColor(awtColor);
                        contentStream.setFont(font, fontSize);
                        
                        String pageNumber = format.replace("{page}", String.valueOf(pageIndex + 1))
                                                .replace("{total}", String.valueOf(totalPages));
                        
                        PDRectangle pageSize = page.getMediaBox();
                        float x = 0, y = 0;
                        
                        switch (position) {
                            case "top-center" -> {
                                x = (pageSize.getWidth() - fontSize * pageNumber.length()) / 2;
                                y = pageSize.getHeight() - margin;
                            }
                            case "bottom-center" -> {
                                x = (pageSize.getWidth() - fontSize * pageNumber.length()) / 2;
                                y = margin;
                            }
                            case "top-right" -> {
                                x = pageSize.getWidth() - fontSize * pageNumber.length() - margin;
                                y = pageSize.getHeight() - margin;
                            }
                            case "bottom-right" -> {
                                x = pageSize.getWidth() - fontSize * pageNumber.length() - margin;
                                y = margin;
                            }
                            case "top-left" -> {
                                x = margin;
                                y = pageSize.getHeight() - margin;
                            }
                            case "bottom-left" -> {
                                x = margin;
                                y = margin;
                            }
                        }
                        
                        contentStream.beginText();
                        contentStream.newLineAtOffset(x, y);
                        contentStream.showText(pageNumber);
                        contentStream.endText();
                    }
                }
                pageIndex++;
            }
            
            document.save(outputFile.toFile());
            return outputFile;
        }
    }
    
    /**
     * Remove pages from PDF
     */
    public Path removePages(Path inputFile, Path outputFile, String pagesToRemove) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            List<PageRange> ranges = parseMultiplePageRanges(pagesToRemove, document.getNumberOfPages());
            Set<Integer> pagesToDelete = new HashSet<>();
            
            for (PageRange range : ranges) {
                for (int i = range.start; i <= range.end; i++) {
                    pagesToDelete.add(i);
                }
            }
            
            // Create new document with remaining pages
            PDDocument newDocument = new PDDocument();
            for (int i = 1; i <= document.getNumberOfPages(); i++) {
                if (!pagesToDelete.contains(i)) {
                    PDPage page = document.getPage(i - 1);
                    newDocument.addPage(page);
                }
            }
            
            newDocument.save(outputFile.toFile());
            newDocument.close();
            return outputFile;
        }
    }
    
    /**
     * Crop PDF pages
     */
    public Path cropPages(Path inputFile, Path outputFile, double left, double top, 
                         double right, double bottom, String pageRange) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            int totalPages = document.getNumberOfPages();
            PageRange range = parsePageRange(pageRange, totalPages);
            
            int pageIndex = 0;
            for (PDPage page : document.getPages()) {
                if (pageIndex >= range.start - 1 && pageIndex < range.end) {
                    PDRectangle mediaBox = page.getMediaBox();
                    
                    float width = mediaBox.getWidth();
                    float height = mediaBox.getHeight();
                    
                    float newLeft = (float) (mediaBox.getLowerLeftX() + (width * left / 100));
                    float newBottom = (float) (mediaBox.getLowerLeftY() + (height * bottom / 100));
                    float newRight = (float) (mediaBox.getUpperRightX() - (width * right / 100));
                    float newTop = (float) (mediaBox.getUpperRightY() - (height * top / 100));
                    
                    PDRectangle cropBox = new PDRectangle(newLeft, newBottom, newRight - newLeft, newTop - newBottom);
                    page.setCropBox(cropBox);
                }
                pageIndex++;
            }
            
            document.save(outputFile.toFile());
            return outputFile;
        }
    }
    
    /**
     * Resize PDF pages
     */
    public Path resizePages(Path inputFile, Path outputFile, Integer width, Integer height,
                           String pageSize, String pageRange, boolean maintainAspectRatio) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            int totalPages = document.getNumberOfPages();
            PageRange range = parsePageRange(pageRange, totalPages);
            
            PDRectangle targetSize = getPageSize(pageSize, width, height);
            
            int pageIndex = 0;
            for (PDPage page : document.getPages()) {
                if (pageIndex >= range.start - 1 && pageIndex < range.end) {
                    page.setMediaBox(targetSize);
                    if (maintainAspectRatio) {
                        page.setCropBox(targetSize);
                    }
                }
                pageIndex++;
            }
            
            document.save(outputFile.toFile());
            return outputFile;
        }
    }
    
    /**
     * Extract images from PDF using PDFBox 3.x API
     */
    public List<BufferedImage> extractImages(Path filePath) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        try (PDDocument document = loadPDF(filePath)) {
            for (PDPage page : document.getPages()) {
                PDResources resources = page.getResources();
                if (resources != null) {
                    for (COSName name : resources.getXObjectNames()) {
                        if (resources.isImageXObject(name)) {
                            PDImageXObject image = (PDImageXObject) resources.getXObject(name);
                            images.add(image.getImage());
                        }
                    }
                }
            }
        }
        return images;
    }
    
    /**
     * Extract metadata
     */
    public Map<String, String> extractMetadata(Path filePath) throws IOException {
        Map<String, String> metadata = new LinkedHashMap<>();
        try (PDDocument document = loadPDF(filePath)) {
            metadata.put("Pages", String.valueOf(document.getNumberOfPages()));
            metadata.put("Encrypted", String.valueOf(document.isEncrypted()));
            metadata.put("Version", String.valueOf(document.getVersion()));
            
            if (document.getDocumentInformation() != null) {
                var info = document.getDocumentInformation();
                if (info.getTitle() != null) metadata.put("Title", info.getTitle());
                if (info.getAuthor() != null) metadata.put("Author", info.getAuthor());
                if (info.getSubject() != null) metadata.put("Subject", info.getSubject());
                if (info.getKeywords() != null) metadata.put("Keywords", info.getKeywords());
                if (info.getCreator() != null) metadata.put("Creator", info.getCreator());
                if (info.getProducer() != null) metadata.put("Producer", info.getProducer());
                if (info.getCreationDate() != null) metadata.put("CreationDate", info.getCreationDate().toString());
                if (info.getModificationDate() != null) metadata.put("ModificationDate", info.getModificationDate().toString());
            }
        }
        return metadata;
    }
    
    /**
     * Edit metadata
     */
    public Path editMetadata(Path inputFile, Path outputFile, Map<String, String> metadata) throws IOException {
        try (PDDocument document = loadPDF(inputFile)) {
            var info = document.getDocumentInformation();
            if (info == null) {
                info = new org.apache.pdfbox.pdmodel.PDDocumentInformation();
            }
            
            // Create final reference for lambda
            final var docInfo = info;
            metadata.forEach((key, value) -> {
                switch (key.toLowerCase()) {
                    case "title" -> docInfo.setTitle(value);
                    case "author" -> docInfo.setAuthor(value);
                    case "subject" -> docInfo.setSubject(value);
                    case "keywords" -> docInfo.setKeywords(value);
                    case "creator" -> docInfo.setCreator(value);
                    case "producer" -> docInfo.setProducer(value);
                    default -> docInfo.setCustomMetadataValue(key, value);
                }
            });
            
            document.setDocumentInformation(docInfo);
            document.save(outputFile.toFile());
            return outputFile;
        }
    }
    
    // Helper methods
    
    private PageRange parsePageRange(String range, int totalPages) {
        if (range == null || range.equals("all")) {
            return new PageRange(1, totalPages);
        }
        
        if (range.contains("-")) {
            String[] parts = range.split("-");
            int start = Integer.parseInt(parts[0]);
            int end = Integer.parseInt(parts[1]);
            return new PageRange(Math.max(1, start), Math.min(totalPages, end));
        } else if (range.contains(",")) {
            String[] parts = range.split(",");
            int start = Integer.parseInt(parts[0]);
            return new PageRange(start, start);
        } else {
            int page = Integer.parseInt(range);
            return new PageRange(page, page);
        }
    }
    
    private List<PageRange> parseMultiplePageRanges(String ranges, int totalPages) {
        List<PageRange> result = new ArrayList<>();
        String[] parts = ranges.split(",");
        for (String part : parts) {
            result.add(parsePageRange(part.trim(), totalPages));
        }
        return result;
    }
    
    /**
     * Get Standard 14 font for PDFBox 3.x
     */
    private PDType1Font getFont(String fontName) {
        return switch (fontName.toUpperCase()) {
            case "HELVETICA" -> new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            case "HELVETICA_BOLD" -> new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            case "HELVETICA_OBLIQUE" -> new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
            case "TIMES_ROMAN" -> new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            case "TIMES_BOLD" -> new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
            case "TIMES_ITALIC" -> new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
            case "COURIER" -> new PDType1Font(Standard14Fonts.FontName.COURIER);
            case "COURIER_BOLD" -> new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
            case "SYMBOL" -> new PDType1Font(Standard14Fonts.FontName.SYMBOL);
            case "ZAPF_DINGBATS" -> new PDType1Font(Standard14Fonts.FontName.ZAPF_DINGBATS);
            default -> new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        };
    }
    
    private PDRectangle getPageSize(String pageSize, Integer width, Integer height) {
        if (width != null && height != null) {
            return new PDRectangle(width, height);
        }
        
        return switch (pageSize.toUpperCase()) {
            case "A4" -> PDRectangle.A4;
            case "LETTER" -> PDRectangle.LETTER;
            case "LEGAL" -> PDRectangle.LEGAL;
            case "A3" -> PDRectangle.A3;
            case "A5" -> PDRectangle.A5;
            default -> PDRectangle.A4;
        };
    }
    
    private void validatePDFFile(Path filePath) throws IOException {
        if (!Files.exists(filePath)) {
            throw new PDFProcessingException("FILE_NOT_FOUND", "PDF file not found: " + filePath);
        }
        
        long fileSizeMB = Files.size(filePath) / (1024 * 1024);
        if (fileSizeMB > maxFileSizeMB) {
            throw new PDFProcessingException("FILE_TOO_LARGE", 
                String.format("PDF file size (%d MB) exceeds maximum allowed (%d MB)", fileSizeMB, maxFileSizeMB));
        }
        
        try (PDDocument doc = Loader.loadPDF(filePath.toFile())) {
            if (doc.getNumberOfPages() > maxPages) {
                throw new PDFProcessingException("TOO_MANY_PAGES", 
                    String.format("PDF has %d pages, maximum allowed is %d", doc.getNumberOfPages(), maxPages));
            }
        }
    }
    
    private static class PageRange {
        final int start;
        final int end;
        
        PageRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}