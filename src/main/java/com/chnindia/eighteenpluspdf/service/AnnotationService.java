package com.chnindia.eighteenpluspdf.service;

import com.chnindia.eighteenpluspdf.dto.request.AnnotationRequest;
import com.chnindia.eighteenpluspdf.dto.request.AnnotationRequest.Annotation;
import com.chnindia.eighteenpluspdf.dto.request.AnnotationRequest.BoundingBox;
import com.chnindia.eighteenpluspdf.dto.request.AnnotationRequest.Point;
import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.interactive.annotation.*;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for PDF annotation operations.
 * Handles highlights, comments, stamps, drawings, and other markup types.
 */
@Service
public class AnnotationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnnotationService.class);
    
    @Autowired
    private FileUtil fileUtil;
    
    /**
     * Add annotations to a PDF document.
     */
    public Map<String, Object> addAnnotations(Path inputFile, AnnotationRequest request) {
        logger.info("Adding {} annotations to PDF: {}", request.getAnnotations().size(), inputFile);
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            int addedCount = 0;
            List<String> errors = new ArrayList<>();
            
            for (Annotation annotation : request.getAnnotations()) {
                try {
                    addAnnotation(document, annotation, request.getAuthor());
                    addedCount++;
                } catch (Exception e) {
                    errors.add("Failed to add annotation on page " + annotation.getPage() + ": " + e.getMessage());
                }
            }
            
            // Save the document
            String outputName = request.getOutputFileName();
            if (outputName == null || outputName.trim().isEmpty()) {
                outputName = "annotated_document";
            }
            
            Path outputPath = fileUtil.createOutputFile(outputName, "pdf");
            document.save(outputPath.toFile());
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "addedAnnotations", addedCount,
                "requestedAnnotations", request.getAnnotations().size(),
                "errors", errors
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("ANNOTATION_ERROR", "Failed to add annotations: " + e.getMessage());
        }
    }
    
    private void addAnnotation(PDDocument document, Annotation annotation, String defaultAuthor) throws IOException {
        int pageIndex = annotation.getPage() - 1; // Convert to 0-based
        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            throw new PDFProcessingException("INVALID_PAGE", "Invalid page number: " + annotation.getPage());
        }
        
        PDPage page = document.getPage(pageIndex);
        PDAnnotation pdAnnotation = createAnnotation(annotation, page);
        
        // Set common properties
        if (pdAnnotation != null) {
            String author = annotation.getAuthor() != null ? annotation.getAuthor() : defaultAuthor;
            
            // These methods are on PDAnnotationMarkup subclass
            if (pdAnnotation instanceof PDAnnotationMarkup markup) {
                if (author != null) {
                    markup.setTitlePopup(author);
                }
                if (annotation.getSubject() != null) {
                    markup.setSubject(annotation.getSubject());
                }
                if (annotation.getContent() != null) {
                    markup.setContents(annotation.getContent());
                }
            }
            
            page.getAnnotations().add(pdAnnotation);
        }
    }
    
    private PDAnnotation createAnnotation(Annotation annotation, PDPage page) throws IOException {
        BoundingBox bbox = annotation.getBoundingBox();
        PDRectangle rect = bbox != null ? 
            new PDRectangle(bbox.getX(), bbox.getY(), bbox.getWidth(), bbox.getHeight()) :
            new PDRectangle(100, 100, 200, 50); // Default
        
        PDColor color = parseColor(annotation.getColor());
        
        switch (annotation.getType()) {
            case HIGHLIGHT:
                return createHighlightAnnotation(rect, color, annotation.getOpacity());
            case UNDERLINE:
                return createUnderlineAnnotation(rect, color);
            case STRIKEOUT:
                return createStrikeoutAnnotation(rect, color);
            case SQUIGGLY:
                return createSquigglyAnnotation(rect, color);
            case TEXT_NOTE:
                return createTextAnnotation(rect, color, annotation.getContent());
            case FREE_TEXT:
                return createFreeTextAnnotation(rect, color, annotation.getContent());
            case LINE:
                return createLineAnnotation(annotation, color);
            case ARROW:
                return createArrowAnnotation(annotation, color);
            case RECTANGLE:
                return createSquareAnnotation(rect, color, annotation.getLineWidth());
            case CIRCLE:
                return createCircleAnnotation(rect, color, annotation.getLineWidth());
            case POLYGON:
                return createPolygonAnnotation(annotation, color);
            case POLYLINE:
                return createPolylineAnnotation(annotation, color);
            case INK:
                return createInkAnnotation(annotation, color);
            case STAMP:
                return createStampAnnotation(rect, annotation.getStampType());
            case LINK:
                return createLinkAnnotation(rect, annotation.getContent());
            default:
                logger.warn("Unsupported annotation type: {}", annotation.getType());
                return null;
        }
    }
    
    private PDAnnotation createHighlightAnnotation(PDRectangle rect, PDColor color, float opacity) {
        // Create a highlight markup annotation using COSDictionary for PDFBox 3.x
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.ANNOT);
        dict.setName(COSName.SUBTYPE, "Highlight");
        
        PDAnnotationMarkup highlight = new PDAnnotationMarkup(dict);
        highlight.setRectangle(rect);
        highlight.setColor(color);
        highlight.setConstantOpacity(opacity);
        
        return highlight;
    }
    
    private PDAnnotation createUnderlineAnnotation(PDRectangle rect, PDColor color) {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.ANNOT);
        dict.setName(COSName.SUBTYPE, "Underline");
        
        PDAnnotationMarkup underline = new PDAnnotationMarkup(dict);
        underline.setRectangle(rect);
        underline.setColor(color);
        
        return underline;
    }
    
    private PDAnnotation createStrikeoutAnnotation(PDRectangle rect, PDColor color) {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.ANNOT);
        dict.setName(COSName.SUBTYPE, "StrikeOut");
        
        PDAnnotationMarkup strikeout = new PDAnnotationMarkup(dict);
        strikeout.setRectangle(rect);
        strikeout.setColor(color);
        
        return strikeout;
    }
    
    private PDAnnotation createSquigglyAnnotation(PDRectangle rect, PDColor color) {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.ANNOT);
        dict.setName(COSName.SUBTYPE, "Squiggly");
        
        PDAnnotationMarkup squiggly = new PDAnnotationMarkup(dict);
        squiggly.setRectangle(rect);
        squiggly.setColor(color);
        
        return squiggly;
    }
    
    private PDAnnotationText createTextAnnotation(PDRectangle rect, PDColor color, String content) {
        PDAnnotationText textNote = new PDAnnotationText();
        textNote.setRectangle(rect);
        textNote.setColor(color);
        if (content != null) {
            textNote.setContents(content);
        }
        textNote.setName(PDAnnotationText.NAME_COMMENT);
        return textNote;
    }
    
    private PDAnnotationFreeText createFreeTextAnnotation(PDRectangle rect, PDColor color, String content) {
        PDAnnotationFreeText freeText = new PDAnnotationFreeText();
        freeText.setRectangle(rect);
        freeText.setColor(color);
        if (content != null) {
            freeText.setContents(content);
        }
        freeText.setDefaultAppearance("/Helv 12 Tf 0 0 0 rg");
        return freeText;
    }
    
    private PDAnnotationLine createLineAnnotation(Annotation annotation, PDColor color) {
        PDAnnotationLine line = new PDAnnotationLine();
        
        BoundingBox bbox = annotation.getBoundingBox();
        if (bbox != null) {
            line.setRectangle(new PDRectangle(bbox.getX(), bbox.getY(), bbox.getWidth(), bbox.getHeight()));
            
            // Set line points
            float[] linePoints = new float[] {
                bbox.getX(), bbox.getY(),
                bbox.getX() + bbox.getWidth(), bbox.getY() + bbox.getHeight()
            };
            line.setLine(linePoints);
        }
        
        line.setColor(color);
        return line;
    }
    
    private PDAnnotationLine createArrowAnnotation(Annotation annotation, PDColor color) {
        PDAnnotationLine arrow = createLineAnnotation(annotation, color);
        arrow.setEndPointEndingStyle(PDAnnotationLine.LE_OPEN_ARROW);
        return arrow;
    }
    
    private PDAnnotation createSquareAnnotation(PDRectangle rect, PDColor color, float lineWidth) {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.ANNOT);
        dict.setName(COSName.SUBTYPE, "Square");
        
        PDAnnotationMarkup square = new PDAnnotationMarkup(dict);
        square.setRectangle(rect);
        square.setColor(color);
        return square;
    }
    
    private PDAnnotation createCircleAnnotation(PDRectangle rect, PDColor color, float lineWidth) {
        COSDictionary dict = new COSDictionary();
        dict.setItem(COSName.TYPE, COSName.ANNOT);
        dict.setName(COSName.SUBTYPE, "Circle");
        
        PDAnnotationMarkup circle = new PDAnnotationMarkup(dict);
        circle.setRectangle(rect);
        circle.setColor(color);
        return circle;
    }
    
    private PDAnnotationPolygon createPolygonAnnotation(Annotation annotation, PDColor color) {
        PDAnnotationPolygon polygon = new PDAnnotationPolygon();
        
        if (annotation.getPoints() != null && !annotation.getPoints().isEmpty()) {
            float[] vertices = new float[annotation.getPoints().size() * 2];
            int i = 0;
            for (Point point : annotation.getPoints()) {
                vertices[i++] = point.getX();
                vertices[i++] = point.getY();
            }
            polygon.setVertices(vertices);
        }
        
        polygon.setColor(color);
        return polygon;
    }
    
    private PDAnnotationPolyline createPolylineAnnotation(Annotation annotation, PDColor color) {
        PDAnnotationPolyline polyline = new PDAnnotationPolyline();
        
        if (annotation.getPoints() != null && !annotation.getPoints().isEmpty()) {
            float[] vertices = new float[annotation.getPoints().size() * 2];
            int i = 0;
            for (Point point : annotation.getPoints()) {
                vertices[i++] = point.getX();
                vertices[i++] = point.getY();
            }
            polyline.setVertices(vertices);
        }
        
        polyline.setColor(color);
        return polyline;
    }
    
    private PDAnnotationMarkup createInkAnnotation(Annotation annotation, PDColor color) {
        // Use PDAnnotationMarkup for ink-like drawing annotation
        PDAnnotationMarkup ink = new PDAnnotationMarkup();
        ink.setColor(color);
        
        if (annotation.getBoundingBox() != null) {
            BoundingBox bbox = annotation.getBoundingBox();
            ink.setRectangle(new PDRectangle(bbox.getX(), bbox.getY(), bbox.getWidth(), bbox.getHeight()));
        }
        
        return ink;
    }
    
    private PDAnnotationRubberStamp createStampAnnotation(PDRectangle rect, Annotation.StampType stampType) {
        PDAnnotationRubberStamp stamp = new PDAnnotationRubberStamp();
        stamp.setRectangle(rect);
        
        String stampName = stampType != null ? stampType.name().replace("_", "") : "Draft";
        stamp.setName(stampName);
        
        return stamp;
    }
    
    private PDAnnotationLink createLinkAnnotation(PDRectangle rect, String uri) {
        PDAnnotationLink link = new PDAnnotationLink();
        link.setRectangle(rect);
        
        if (uri != null) {
            org.apache.pdfbox.pdmodel.interactive.action.PDActionURI action = 
                new org.apache.pdfbox.pdmodel.interactive.action.PDActionURI();
            action.setURI(uri);
            link.setAction(action);
        }
        
        return link;
    }
    
    private PDColor parseColor(String hexColor) {
        if (hexColor == null || hexColor.isEmpty()) {
            return new PDColor(new float[]{1f, 1f, 0f}, PDDeviceRGB.INSTANCE); // Yellow default
        }
        
        try {
            Color color = Color.decode(hexColor);
            return new PDColor(
                new float[]{color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f},
                PDDeviceRGB.INSTANCE
            );
        } catch (NumberFormatException e) {
            return new PDColor(new float[]{1f, 1f, 0f}, PDDeviceRGB.INSTANCE);
        }
    }
    
    /**
     * List all annotations in a PDF document.
     */
    public Map<String, Object> listAnnotations(Path inputFile) {
        logger.info("Listing annotations in PDF: {}", inputFile);
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            List<Map<String, Object>> annotations = new ArrayList<>();
            int totalCount = 0;
            
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                
                for (PDAnnotation annotation : page.getAnnotations()) {
                    Map<String, Object> annotationInfo = new LinkedHashMap<>();
                    annotationInfo.put("page", pageIndex + 1);
                    annotationInfo.put("type", annotation.getSubtype());
                    
                    // These properties are on PDAnnotationMarkup subclass
                    if (annotation instanceof PDAnnotationMarkup markup) {
                        annotationInfo.put("author", markup.getTitlePopup());
                        annotationInfo.put("subject", markup.getSubject());
                        annotationInfo.put("content", markup.getContents());
                    }
                    
                    PDRectangle rect = annotation.getRectangle();
                    if (rect != null) {
                        annotationInfo.put("x", rect.getLowerLeftX());
                        annotationInfo.put("y", rect.getLowerLeftY());
                        annotationInfo.put("width", rect.getWidth());
                        annotationInfo.put("height", rect.getHeight());
                    }
                    
                    annotationInfo.put("modifiedDate", annotation.getModifiedDate());
                    
                    annotations.add(annotationInfo);
                    totalCount++;
                }
            }
            
            return Map.of(
                "totalAnnotations", totalCount,
                "pageCount", document.getNumberOfPages(),
                "annotations", annotations
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("LIST_ANNOTATION_ERROR", "Failed to list annotations: " + e.getMessage());
        }
    }
    
    /**
     * Remove annotations from a PDF document.
     */
    public Map<String, Object> removeAnnotations(Path inputFile, List<String> types, List<Integer> pages, String outputFileName) {
        logger.info("Removing annotations from PDF: {}", inputFile);
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            int removedCount = 0;
            Set<String> typeSet = types != null ? new HashSet<>(types) : null;
            Set<Integer> pageSet = pages != null ? new HashSet<>(pages) : null;
            
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                // Skip if specific pages are requested and this page is not included
                if (pageSet != null && !pageSet.contains(pageIndex + 1)) {
                    continue;
                }
                
                PDPage page = document.getPage(pageIndex);
                List<PDAnnotation> toKeep = new ArrayList<>();
                
                for (PDAnnotation annotation : page.getAnnotations()) {
                    boolean remove = false;
                    
                    if (typeSet == null || typeSet.isEmpty()) {
                        // Remove all annotations (except form widgets)
                        remove = !(annotation instanceof PDAnnotationWidget);
                    } else {
                        // Remove only specified types
                        remove = typeSet.contains(annotation.getSubtype());
                    }
                    
                    if (remove) {
                        removedCount++;
                    } else {
                        toKeep.add(annotation);
                    }
                }
                
                page.setAnnotations(toKeep);
            }
            
            // Save the document
            if (outputFileName == null || outputFileName.trim().isEmpty()) {
                outputFileName = "cleaned_document";
            }
            
            Path outputPath = fileUtil.createOutputFile(outputFileName, "pdf");
            document.save(outputPath.toFile());
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "removedAnnotations", removedCount
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("REMOVE_ANNOTATION_ERROR", "Failed to remove annotations: " + e.getMessage());
        }
    }
    
    /**
     * Flatten annotations into the page content.
     */
    public Map<String, Object> flattenAnnotations(Path inputFile, String outputFileName) {
        logger.info("Flattening annotations in PDF: {}", inputFile);
        
        try (PDDocument document = Loader.loadPDF(inputFile.toFile())) {
            int flattenedCount = 0;
            
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);
                List<PDAnnotation> annotations = page.getAnnotations();
                
                // Count non-widget annotations
                for (PDAnnotation annotation : annotations) {
                    if (!(annotation instanceof PDAnnotationWidget)) {
                        flattenedCount++;
                    }
                }
                
                // Remove annotations after flattening (PDFBox handles this on save with proper settings)
                List<PDAnnotation> widgets = annotations.stream()
                    .filter(a -> a instanceof PDAnnotationWidget)
                    .collect(Collectors.toList());
                page.setAnnotations(widgets);
            }
            
            // Save the document
            if (outputFileName == null || outputFileName.trim().isEmpty()) {
                outputFileName = "flattened_document";
            }
            
            Path outputPath = fileUtil.createOutputFile(outputFileName, "pdf");
            document.save(outputPath.toFile());
            
            return Map.of(
                "resultUrl", fileUtil.getDownloadUrl(outputPath.getFileName().toString()),
                "flattenedAnnotations", flattenedCount
            );
            
        } catch (IOException e) {
            throw new PDFProcessingException("FLATTEN_ANNOTATION_ERROR", "Failed to flatten annotations: " + e.getMessage());
        }
    }
}
