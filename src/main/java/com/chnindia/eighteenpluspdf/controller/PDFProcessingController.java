package com.chnindia.eighteenpluspdf.controller;

import com.chnindia.eighteenpluspdf.dto.response.PDFProcessingResponse;
import com.chnindia.eighteenpluspdf.dto.response.JobStatusResponse;
import com.chnindia.eighteenpluspdf.service.JobQueueService;
import com.chnindia.eighteenpluspdf.util.ValidationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/pdf")
@Tag(name = "PDF Processing", description = "Complete PDF processing API with 32 tools")
public class PDFProcessingController {
    
    @Autowired
    private JobQueueService jobQueueService;
    
    @PostMapping("/merge")
    @Operation(summary = "Merge multiple PDFs", description = "Merge multiple PDF files into a single PDF document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued", 
            content = @Content(schema = @Schema(implementation = PDFProcessingResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key"),
        @ApiResponse(responseCode = "413", description = "File size exceeds limit")
    })
    public ResponseEntity<PDFProcessingResponse> mergePDFs(
            @Parameter(description = "PDF files to merge", required = true)
            @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "Output filename (optional)")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Preserve bookmarks (default: true)")
            @RequestParam(defaultValue = "true") Boolean preserveBookmarks,
            @Parameter(description = "Remove annotations (default: false)")
            @RequestParam(defaultValue = "false") Boolean removeAnnotations) {
        
        ValidationUtil.validateMultipleFiles(files, 2, 50);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("merge");
        jobRequest.setFile(files[0]);
        jobRequest.setParameters(Map.of(
            "files", java.util.Arrays.asList(files),
            "outputFileName", outputFileName,
            "preserveBookmarks", preserveBookmarks,
            "removeAnnotations", removeAnnotations
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/split")
    @Operation(summary = "Split PDF", description = "Split PDF into multiple files")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> splitPDF(
            @Parameter(description = "PDF file to split", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Pages per file (default: 1)")
            @RequestParam(required = false) Integer pagesPerFile,
            @Parameter(description = "Page ranges (e.g., '1-3,5,7-9')")
            @RequestParam(required = false) String pageRanges,
            @Parameter(description = "Output file prefix")
            @RequestParam(required = false) String outputPrefix) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("split");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pagesPerFile", pagesPerFile,
            "pageRanges", pageRanges,
            "outputPrefix", outputPrefix
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/compress")
    @Operation(summary = "Compress PDF", description = "Reduce PDF file size")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> compressPDF(
            @Parameter(description = "PDF file to compress", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Compression quality (0.1-1.0)")
            @RequestParam(required = false) Double compressionQuality,
            @Parameter(description = "Remove metadata")
            @RequestParam(defaultValue = "false") Boolean removeMetadata,
            @Parameter(description = "Optimize images")
            @RequestParam(defaultValue = "true") Boolean optimizeImages,
            @Parameter(description = "Max image DPI")
            @RequestParam(required = false) Integer maxImageDpi,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        if (compressionQuality != null) ValidationUtil.validateCompressionQuality(compressionQuality);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("compress");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "compressionQuality", compressionQuality,
            "removeMetadata", removeMetadata,
            "optimizeImages", optimizeImages,
            "maxImageDpi", maxImageDpi,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/rotate")
    @Operation(summary = "Rotate PDF pages", description = "Rotate pages in a PDF document")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> rotatePDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Rotation angle (90, 180, 270)", required = true)
            @RequestParam Integer angle,
            @Parameter(description = "Page range (default: all)")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateRotationAngle(angle);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("rotate");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "angle", angle,
            "pageRange", pageRange,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/watermark")
    @Operation(summary = "Add watermark", description = "Add watermark to PDF pages")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> addWatermark(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Watermark text", required = true)
            @RequestParam String watermarkText,
            @Parameter(description = "Font name (default: Helvetica)")
            @RequestParam(defaultValue = "Helvetica") String fontName,
            @Parameter(description = "Font size (default: 48)")
            @RequestParam(defaultValue = "48") Integer fontSize,
            @Parameter(description = "Color hex (default: #808080)")
            @RequestParam(defaultValue = "#808080") String color,
            @Parameter(description = "Opacity (0.0-1.0, default: 0.3)")
            @RequestParam(defaultValue = "0.3") Double opacity,
            @Parameter(description = "Position (center, top-left, etc.)")
            @RequestParam(defaultValue = "center") String position,
            @Parameter(description = "Rotation angle")
            @RequestParam(defaultValue = "45") Integer rotation,
            @Parameter(description = "Diagonal placement")
            @RequestParam(defaultValue = "true") Boolean diagonal,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateColor(color);
        ValidationUtil.validateOpacity(opacity);
        ValidationUtil.validateFontSize(fontSize);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("watermark");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "watermarkText", watermarkText,
            "fontName", fontName,
            "fontSize", fontSize,
            "color", color,
            "opacity", opacity,
            "position", position,
            "rotation", rotation,
            "diagonal", diagonal,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/encrypt")
    @Operation(summary = "Encrypt PDF", description = "Add password protection to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> encryptPDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Owner password", required = true)
            @RequestParam String ownerPassword,
            @Parameter(description = "User password (optional)")
            @RequestParam(required = false) String userPassword,
            @Parameter(description = "Allow printing")
            @RequestParam(defaultValue = "true") Boolean allowPrint,
            @Parameter(description = "Allow copying")
            @RequestParam(defaultValue = "true") Boolean allowCopy,
            @Parameter(description = "Allow modification")
            @RequestParam(defaultValue = "true") Boolean allowModify,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validatePasswordStrength(ownerPassword);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("encrypt");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "ownerPassword", ownerPassword,
            "userPassword", userPassword,
            "allowPrint", allowPrint,
            "allowCopy", allowCopy,
            "allowModify", allowModify,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/decrypt")
    @Operation(summary = "Decrypt PDF", description = "Remove password protection from PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> decryptPDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Password", required = true)
            @RequestParam String password,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("decrypt");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "password", password,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/extract-text")
    @Operation(summary = "Extract text", description = "Extract text content from PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> extractText(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("extract-text");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of());
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/extract-images")
    @Operation(summary = "Extract images", description = "Extract images from PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> extractImages(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("extract-images");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of());
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/extract-metadata")
    @Operation(summary = "Extract metadata", description = "Extract metadata from PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> extractMetadata(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("extract-metadata");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of());
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/add-page-numbers")
    @Operation(summary = "Add page numbers", description = "Add page numbers to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> addPageNumbers(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Format (default: 'Page {page} of {total}')")
            @RequestParam(defaultValue = "Page {page} of {total}") String format,
            @Parameter(description = "Font name")
            @RequestParam(defaultValue = "Helvetica") String fontName,
            @Parameter(description = "Font size")
            @RequestParam(defaultValue = "12") Integer fontSize,
            @Parameter(description = "Color hex")
            @RequestParam(defaultValue = "#000000") String color,
            @Parameter(description = "Position")
            @RequestParam(defaultValue = "bottom-center") String position,
            @Parameter(description = "Margin")
            @RequestParam(defaultValue = "36") Integer margin,
            @Parameter(description = "Start page")
            @RequestParam(defaultValue = "all") String startPage,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateFontSize(fontSize);
        ValidationUtil.validateMargin(margin);
        ValidationUtil.validateColor(color);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("add-page-numbers");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "format", format,
            "fontName", fontName,
            "fontSize", fontSize,
            "color", color,
            "position", position,
            "margin", margin,
            "startPage", startPage,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/remove-pages")
    @Operation(summary = "Remove pages", description = "Remove pages from PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> removePages(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Pages to remove (e.g., '1-3,5')", required = true)
            @RequestParam String pagesToRemove,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("remove-pages");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pagesToRemove", pagesToRemove,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/crop-pages")
    @Operation(summary = "Crop pages", description = "Crop pages in PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> cropPages(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Left margin (%)")
            @RequestParam(defaultValue = "0.0") Double left,
            @Parameter(description = "Top margin (%)")
            @RequestParam(defaultValue = "0.0") Double top,
            @Parameter(description = "Right margin (%)")
            @RequestParam(defaultValue = "0.0") Double right,
            @Parameter(description = "Bottom margin (%)")
            @RequestParam(defaultValue = "0.0") Double bottom,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("crop-pages");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "left", left,
            "top", top,
            "right", right,
            "bottom", bottom,
            "pageRange", pageRange,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/resize-pages")
    @Operation(summary = "Resize pages", description = "Resize pages in PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> resizePages(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Width in points")
            @RequestParam(required = false) Integer width,
            @Parameter(description = "Height in points")
            @RequestParam(required = false) Integer height,
            @Parameter(description = "Page size (A4, Letter, etc.)")
            @RequestParam(defaultValue = "A4") String pageSize,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Maintain aspect ratio")
            @RequestParam(defaultValue = "true") Boolean maintainAspectRatio,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validatePageSize(pageSize);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("resize-pages");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "width", width,
            "height", height,
            "pageSize", pageSize,
            "pageRange", pageRange,
            "maintainAspectRatio", maintainAspectRatio,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/pdf-to-image")
    @Operation(summary = "PDF to Image", description = "Convert PDF pages to images")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> pdfToImage(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Image format (png, jpg, tiff)")
            @RequestParam(defaultValue = "png") String imageFormat,
            @Parameter(description = "DPI resolution")
            @RequestParam(defaultValue = "300") Integer dpi,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output prefix")
            @RequestParam(required = false) String outputPrefix) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateDPI(dpi);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("pdf-to-image");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "imageFormat", imageFormat,
            "dpi", dpi,
            "pageRange", pageRange,
            "outputPrefix", outputPrefix
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/image-to-pdf")
    @Operation(summary = "Image to PDF", description = "Convert images to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> imageToPDF(
            @Parameter(description = "Image files", required = true)
            @RequestParam("images") MultipartFile[] images,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "A4") String pageSize,
            @Parameter(description = "Fit to page")
            @RequestParam(defaultValue = "true") Boolean fitToPage,
            @Parameter(description = "Margin")
            @RequestParam(defaultValue = "10") Integer margin) {
        
        ValidationUtil.validateMultipleFiles(images, 1, 100);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("image-to-pdf");
        jobRequest.setFile(images[0]);
        jobRequest.setParameters(Map.of(
            "images", java.util.Arrays.asList(images),
            "outputFileName", outputFileName,
            "pageSize", pageSize,
            "fitToPage", fitToPage,
            "margin", margin
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/pdf-to-text")
    @Operation(summary = "PDF to Text", description = "Convert PDF to text file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> pdfToText(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("pdf-to-text");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pageRange", pageRange,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/pdf-to-word")
    @Operation(summary = "PDF to Word", description = "Convert PDF to Word document (DOCX)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> pdfToWord(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("pdf-to-word");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pageRange", pageRange,
            "outputFileName", outputFileName != null ? outputFileName : "converted"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/pdf-to-excel")
    @Operation(summary = "PDF to Excel", description = "Convert PDF tables to Excel spreadsheet (XLSX)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> pdfToExcel(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("pdf-to-excel");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pageRange", pageRange,
            "outputFileName", outputFileName != null ? outputFileName : "converted"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/pdf-to-ppt")
    @Operation(summary = "PDF to PowerPoint", description = "Convert PDF to PowerPoint presentation (PPTX)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> pdfToPowerPoint(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("pdf-to-ppt");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pageRange", pageRange,
            "outputFileName", outputFileName != null ? outputFileName : "converted"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/word-to-pdf")
    @Operation(summary = "Word to PDF", description = "Convert Word document to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> wordToPdf(
            @Parameter(description = "Word file (DOC/DOCX)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("word-to-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "outputFileName", outputFileName != null ? outputFileName : "converted"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/excel-to-pdf")
    @Operation(summary = "Excel to PDF", description = "Convert Excel spreadsheet to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> excelToPdf(
            @Parameter(description = "Excel file (XLS/XLSX)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("excel-to-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "outputFileName", outputFileName != null ? outputFileName : "converted"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/ppt-to-pdf")
    @Operation(summary = "PowerPoint to PDF", description = "Convert PowerPoint presentation to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> pptToPdf(
            @Parameter(description = "PowerPoint file (PPT/PPTX)", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("ppt-to-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "outputFileName", outputFileName != null ? outputFileName : "converted"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/text-to-pdf")
    @Operation(summary = "Text to PDF", description = "Convert text file to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> textToPDF(
            @Parameter(description = "Text file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Font name")
            @RequestParam(defaultValue = "Helvetica") String fontName,
            @Parameter(description = "Font size")
            @RequestParam(defaultValue = "12") Integer fontSize,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "A4") String pageSize,
            @Parameter(description = "Margin")
            @RequestParam(defaultValue = "36") Integer margin,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateFontSize(fontSize);
        ValidationUtil.validateMargin(margin);
        ValidationUtil.validatePageSize(pageSize);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("text-to-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "fontName", fontName,
            "fontSize", fontSize,
            "pageSize", pageSize,
            "margin", margin,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/office-to-pdf")
    @Operation(summary = "Office to PDF", description = "Convert Office documents to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> officeToPDF(
            @Parameter(description = "Office file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Hide comments")
            @RequestParam(defaultValue = "false") Boolean hideComments,
            @Parameter(description = "Export bookmarks")
            @RequestParam(defaultValue = "true") Boolean exportBookmarks,
            @Parameter(description = "Timeout seconds")
            @RequestParam(defaultValue = "60") Integer timeoutSeconds) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("office-to-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "outputFileName", outputFileName,
            "hideComments", hideComments,
            "exportBookmarks", exportBookmarks,
            "timeoutSeconds", timeoutSeconds
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/html-to-pdf")
    @Operation(summary = "HTML to PDF", description = "Convert HTML to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> htmlToPDF(
            @Parameter(description = "HTML file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "A4") String pageSize,
            @Parameter(description = "Margin")
            @RequestParam(defaultValue = "20") Integer margin,
            @Parameter(description = "Enable images")
            @RequestParam(defaultValue = "true") Boolean enableImages,
            @Parameter(description = "Enable JavaScript")
            @RequestParam(defaultValue = "false") Boolean enableJavaScript,
            @Parameter(description = "Timeout seconds")
            @RequestParam(defaultValue = "30") Integer timeoutSeconds) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("html-to-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "outputFileName", outputFileName,
            "pageSize", pageSize,
            "margin", margin,
            "enableImages", enableImages,
            "enableJavaScript", enableJavaScript,
            "timeoutSeconds", timeoutSeconds
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/markdown-to-pdf")
    @Operation(summary = "Markdown to PDF", description = "Convert Markdown to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> markdownToPDF(
            @Parameter(description = "Markdown file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "A4") String pageSize,
            @Parameter(description = "Margin")
            @RequestParam(defaultValue = "36") Integer margin,
            @Parameter(description = "Theme")
            @RequestParam(defaultValue = "default") String theme,
            @Parameter(description = "Enable code highlighting")
            @RequestParam(defaultValue = "true") Boolean enableCodeHighlighting) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("markdown-to-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "outputFileName", outputFileName,
            "pageSize", pageSize,
            "margin", margin,
            "theme", theme,
            "enableCodeHighlighting", enableCodeHighlighting
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/txt-to-pdf")
    @Operation(summary = "Text to PDF", description = "Convert text file to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> txtToPDF(
            @Parameter(description = "Text file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        return textToPDF(file, "Helvetica", 12, "A4", 36, outputFileName);
    }
    
    @PostMapping("/ocr-pdf")
    @Operation(summary = "OCR PDF", description = "Perform OCR on PDF to make it searchable")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> ocrPDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Language (eng, spa, fra, etc.)")
            @RequestParam(defaultValue = "eng") String language,
            @Parameter(description = "DPI resolution")
            @RequestParam(defaultValue = "300") Integer dpi,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Make searchable")
            @RequestParam(defaultValue = "true") Boolean makeSearchable,
            @Parameter(description = "Preserve original")
            @RequestParam(defaultValue = "false") Boolean preserveOriginal) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateOCRLanguage(language);
        ValidationUtil.validateDPI(dpi);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("ocr-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "language", language,
            "dpi", dpi,
            "outputFileName", outputFileName,
            "makeSearchable", makeSearchable,
            "preserveOriginal", preserveOriginal
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/compare-pdfs")
    @Operation(summary = "Compare PDFs", description = "Compare two PDF files")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> comparePDFs(
            @Parameter(description = "First PDF file", required = true)
            @RequestParam("file1") MultipartFile file1,
            @Parameter(description = "Second PDF file", required = true)
            @RequestParam("file2") MultipartFile file2,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Compare text")
            @RequestParam(defaultValue = "true") Boolean compareText,
            @Parameter(description = "Compare images")
            @RequestParam(defaultValue = "true") Boolean compareImages,
            @Parameter(description = "Compare layout")
            @RequestParam(defaultValue = "true") Boolean compareLayout,
            @Parameter(description = "Tolerance")
            @RequestParam(defaultValue = "0.1") Double tolerance) {
        
        ValidationUtil.validateFileNotNull(file1, "file1");
        ValidationUtil.validateFileNotNull(file2, "file2");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("compare-pdfs");
        jobRequest.setFile(file1);
        jobRequest.setParameters(Map.of(
            "file2", file2,
            "outputFileName", outputFileName,
            "compareText", compareText,
            "compareImages", compareImages,
            "compareLayout", compareLayout,
            "tolerance", tolerance
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/pdfa-convert")
    @Operation(summary = "Convert to PDF/A", description = "Convert PDF to PDF/A format")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> pdfaConvert(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Compliance level (1b, 2b, 2u, 3b)")
            @RequestParam(defaultValue = "2b") String complianceLevel,
            @Parameter(description = "Embed fonts")
            @RequestParam(defaultValue = "true") Boolean embedFonts,
            @Parameter(description = "Validate output")
            @RequestParam(defaultValue = "true") Boolean validateOutput,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateComplianceLevel(complianceLevel);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("pdfa-convert");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "complianceLevel", complianceLevel,
            "embedFonts", embedFonts,
            "validateOutput", validateOutput,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/linearize")
    @Operation(summary = "Linearize PDF", description = "Optimize PDF for web (linearize)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> linearize(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName,
            @Parameter(description = "Optimize for web")
            @RequestParam(defaultValue = "true") Boolean optimizeForWeb) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("linearize");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "outputFileName", outputFileName,
            "optimizeForWeb", optimizeForWeb
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/optimize")
    @Operation(summary = "Optimize PDF", description = "Optimize PDF for size and performance")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> optimize(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Remove unused objects")
            @RequestParam(defaultValue = "true") Boolean removeUnusedObjects,
            @Parameter(description = "Compress images")
            @RequestParam(defaultValue = "true") Boolean compressImages,
            @Parameter(description = "Image quality (1-100)")
            @RequestParam(defaultValue = "85") Integer imageQuality,
            @Parameter(description = "Subset fonts")
            @RequestParam(defaultValue = "true") Boolean subsetFonts,
            @Parameter(description = "Linearize")
            @RequestParam(defaultValue = "false") Boolean linearize,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("optimize");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "removeUnusedObjects", removeUnusedObjects,
            "compressImages", compressImages,
            "imageQuality", imageQuality,
            "subsetFonts", subsetFonts,
            "linearize", linearize,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/metadata-edit")
    @Operation(summary = "Edit metadata", description = "Edit PDF metadata")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> editMetadata(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Title")
            @RequestParam(required = false) String title,
            @Parameter(description = "Author")
            @RequestParam(required = false) String author,
            @Parameter(description = "Subject")
            @RequestParam(required = false) String subject,
            @Parameter(description = "Keywords")
            @RequestParam(required = false) String keywords,
            @Parameter(description = "Creator")
            @RequestParam(required = false) String creator,
            @Parameter(description = "Producer")
            @RequestParam(required = false) String producer,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("metadata-edit");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "title", title,
            "author", author,
            "subject", subject,
            "keywords", keywords,
            "creator", creator,
            "producer", producer,
            "outputFileName", outputFileName
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    // ==================== SECURITY ENDPOINTS ====================
    
    @PostMapping("/sign-pdf")
    @Operation(summary = "Sign PDF", description = "Add digital signature to PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> signPDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Reason for signing")
            @RequestParam(required = false) String reason,
            @Parameter(description = "Location of signing")
            @RequestParam(required = false) String location,
            @Parameter(description = "Contact information")
            @RequestParam(required = false) String contact,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("sign-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "reason", reason != null ? reason : "Document signed",
            "location", location != null ? location : "",
            "contact", contact != null ? contact : "",
            "outputFileName", outputFileName != null ? outputFileName : "signed_document"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/verify-signature")
    @Operation(summary = "Verify signature", description = "Verify digital signatures in PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> verifySignature(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Verify timestamp")
            @RequestParam(defaultValue = "true") Boolean verifyTimestamp,
            @Parameter(description = "Check certificate validity")
            @RequestParam(defaultValue = "true") Boolean checkCertificateValidity) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("verify-signature");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "verifyTimestamp", verifyTimestamp,
            "checkCertificateValidity", checkCertificateValidity
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    // ==================== REDACTION & CLEANUP ENDPOINTS ====================
    
    @PostMapping("/redact-pdf")
    @Operation(summary = "Redact PDF", description = "Remove sensitive content from PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> redactPDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Terms to search and redact")
            @RequestParam(required = false) java.util.List<String> searchTerms,
            @Parameter(description = "Redaction color (hex)")
            @RequestParam(defaultValue = "#000000") String redactionColor,
            @Parameter(description = "Remove metadata")
            @RequestParam(defaultValue = "true") Boolean removeMetadata,
            @Parameter(description = "Remove comments")
            @RequestParam(defaultValue = "true") Boolean removeComments,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateColor(redactionColor);
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("redact-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "searchTerms", searchTerms != null ? searchTerms : java.util.List.of(),
            "redactionColor", redactionColor,
            "removeMetadata", removeMetadata,
            "removeComments", removeComments,
            "pageRange", pageRange,
            "outputFileName", outputFileName != null ? outputFileName : "redacted_document"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/flatten-pdf")
    @Operation(summary = "Flatten PDF", description = "Flatten forms and annotations in PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> flattenPDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Flatten form fields")
            @RequestParam(defaultValue = "true") Boolean flattenForms,
            @Parameter(description = "Flatten annotations")
            @RequestParam(defaultValue = "true") Boolean flattenAnnotations,
            @Parameter(description = "Flatten comments")
            @RequestParam(defaultValue = "true") Boolean flattenComments,
            @Parameter(description = "Page range")
            @RequestParam(defaultValue = "all") String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("flatten-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "flattenForms", flattenForms,
            "flattenAnnotations", flattenAnnotations,
            "flattenComments", flattenComments,
            "pageRange", pageRange,
            "outputFileName", outputFileName != null ? outputFileName : "flattened_document"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/repair-pdf")
    @Operation(summary = "Repair PDF", description = "Attempt to repair corrupted PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> repairPDF(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Rebuild xref table")
            @RequestParam(defaultValue = "true") Boolean rebuildXref,
            @Parameter(description = "Remove corrupted objects")
            @RequestParam(defaultValue = "true") Boolean removeCorruptedObjects,
            @Parameter(description = "Validate after repair")
            @RequestParam(defaultValue = "true") Boolean validateAfterRepair,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("repair-pdf");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "rebuildXref", rebuildXref,
            "removeCorruptedObjects", removeCorruptedObjects,
            "validateAfterRepair", validateAfterRepair,
            "outputFileName", outputFileName != null ? outputFileName : "repaired_document"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    // ==================== PAGE MANIPULATION ENDPOINTS ====================
    
    @PostMapping("/reorder-pages")
    @Operation(summary = "Reorder pages", description = "Reorder pages in PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> reorderPages(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "New page order (e.g., [3,1,2])", required = true)
            @RequestParam java.util.List<Integer> pageOrder,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("reorder-pages");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pageOrder", pageOrder,
            "outputFileName", outputFileName != null ? outputFileName : "reordered_document"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/insert-pages")
    @Operation(summary = "Insert pages", description = "Insert pages from one PDF into another")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> insertPages(
            @Parameter(description = "Target PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Source PDF file to insert", required = true)
            @RequestParam("sourcePdf") MultipartFile sourcePdf,
            @Parameter(description = "Insert after page number", required = true)
            @RequestParam Integer insertAfterPage,
            @Parameter(description = "Pages to insert from source")
            @RequestParam(defaultValue = "all") String sourcePageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        ValidationUtil.validateFileNotNull(sourcePdf, "sourcePdf");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("insert-pages");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "sourcePdf", sourcePdf,
            "insertAfterPage", insertAfterPage,
            "sourcePageRange", sourcePageRange,
            "outputFileName", outputFileName != null ? outputFileName : "merged_document"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    @PostMapping("/extract-pages")
    @Operation(summary = "Extract pages", description = "Extract specific pages from PDF")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Job accepted and queued"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    })
    public ResponseEntity<PDFProcessingResponse> extractPages(
            @Parameter(description = "PDF file", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Pages to extract (e.g., '1-3,5,7-9')", required = true)
            @RequestParam String pageRange,
            @Parameter(description = "Output filename")
            @RequestParam(required = false) String outputFileName) {
        
        ValidationUtil.validateFileNotNull(file, "file");
        
        com.chnindia.eighteenpluspdf.dto.JobRequest jobRequest = new com.chnindia.eighteenpluspdf.dto.JobRequest();
        jobRequest.setToolName("extract-pages");
        jobRequest.setFile(file);
        jobRequest.setParameters(Map.of(
            "pageRange", pageRange,
            "outputFileName", outputFileName != null ? outputFileName : "extracted_pages"
        ));
        
        var response = jobQueueService.submitJob(jobRequest);
        return ResponseEntity.accepted().body(new PDFProcessingResponse(response));
    }
    
    // ==================== JOB MANAGEMENT ENDPOINTS ====================
    
    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job status", description = "Get the status of a job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job status retrieved"),
        @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId) {
        
        JobStatusResponse response = jobQueueService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Cancel job", description = "Cancel a running job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Job cancelled"),
        @ApiResponse(responseCode = "404", description = "Job not found"),
        @ApiResponse(responseCode = "400", description = "Cannot cancel job")
    })
    public ResponseEntity<Map<String, Boolean>> cancelJob(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId) {
        
        boolean cancelled = jobQueueService.cancelJob(jobId);
        return ResponseEntity.ok(Map.of("cancelled", cancelled));
    }
    
    @GetMapping("/jobs")
    @Operation(summary = "List jobs", description = "List all jobs or filter by status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Jobs retrieved")
    })
    public ResponseEntity<?> listJobs(
            @Parameter(description = "Status filter")
            @RequestParam(required = false) String status) {
        
        com.chnindia.eighteenpluspdf.model.JobStatus.Status statusEnum = null;
        if (status != null) {
            statusEnum = com.chnindia.eighteenpluspdf.model.JobStatus.Status.valueOf(status.toUpperCase());
        }
        
        var jobs = jobQueueService.getJobs(statusEnum);
        return ResponseEntity.ok(jobs);
    }
    
    @GetMapping("/stats")
    @Operation(summary = "Get statistics", description = "Get job statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved")
    })
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(jobQueueService.getStatistics());
    }
}