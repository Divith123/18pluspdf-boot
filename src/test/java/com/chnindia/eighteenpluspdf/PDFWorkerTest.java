package com.chnindia.eighteenpluspdf;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import com.chnindia.eighteenpluspdf.model.JobStatus;
import com.chnindia.eighteenpluspdf.service.JobQueueService;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import com.chnindia.eighteenpluspdf.util.PDFUtil;
import com.chnindia.eighteenpluspdf.worker.PDFWorker;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PDFWorkerTest {
    
    private PDFWorker pdfWorker;
    private JobStatus jobStatus;
    private JobQueueService mockJobQueueService;
    private FileUtil fileUtil;
    private PDFUtil pdfUtil;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() throws IOException {
        pdfWorker = new PDFWorker();
        Path outputPath = tempDir.resolve("output");
        Files.createDirectories(outputPath);
        
        // Create mock for JobQueueService (not actually calling it)
        mockJobQueueService = mock(JobQueueService.class);
        
        // Create real FileUtil and PDFUtil instances
        fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "tempDir", tempDir.toString());
        ReflectionTestUtils.setField(fileUtil, "outputDir", outputPath.toString());
        ReflectionTestUtils.setField(fileUtil, "allowedExtensions", ".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.bmp,.tiff,.txt");
        
        pdfUtil = new PDFUtil();
        ReflectionTestUtils.setField(pdfUtil, "maxPages", 2000);
        ReflectionTestUtils.setField(pdfUtil, "maxFileSizeMB", 500);
        
        // Inject dependencies
        ReflectionTestUtils.setField(pdfWorker, "jobQueueService", mockJobQueueService);
        ReflectionTestUtils.setField(pdfWorker, "fileUtil", fileUtil);
        ReflectionTestUtils.setField(pdfWorker, "pdfUtil", pdfUtil);
        ReflectionTestUtils.setField(pdfWorker, "tempDir", tempDir.toString());
        ReflectionTestUtils.setField(pdfWorker, "outputDir", outputPath.toString());
        ReflectionTestUtils.setField(pdfWorker, "ocrDpi", 300);
        ReflectionTestUtils.setField(pdfWorker, "maxPages", 1000);
        ReflectionTestUtils.setField(pdfWorker, "imageDpi", 300);
        ReflectionTestUtils.setField(pdfWorker, "compressionQuality", 0.85);
        ReflectionTestUtils.setField(pdfWorker, "ocrTimeout", 60);
        ReflectionTestUtils.setField(pdfWorker, "tesseractPath", "tesseract");
        ReflectionTestUtils.setField(pdfWorker, "tesseractDataPath", "");
        ReflectionTestUtils.setField(pdfWorker, "libreofficePath", "soffice");
        
        // Create a mock JobStatus
        jobStatus = new JobStatus();
        jobStatus.setId("test-job-001");
        jobStatus.setToolName("test");
        jobStatus.setStatus(JobStatus.Status.PROCESSING);
    }
    
    @Test
    void testExtractText() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> result = pdfWorker.process("extract-text", pdfPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl") || result.containsKey("textLength"));
    }
    
    @Test
    void testExtractMetadata() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> result = pdfWorker.process("extract-metadata", pdfPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("metadata") || result.containsKey("extracted"));
    }
    
    @Test
    void testAddPageNumbers() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> result = pdfWorker.process("add-page-numbers", pdfPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testPDFToImage() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> params = new HashMap<>();
        params.put("format", "png");
        params.put("dpi", 150);
        
        Map<String, Object> result = pdfWorker.process("pdf-to-image", pdfPath, params, jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("imageCount") || result.containsKey("imageUrls"));
    }
    
    @Test
    void testPDFToText() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> result = pdfWorker.process("pdf-to-text", pdfPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl") || result.containsKey("textLength"));
    }
    
    @Test
    void testTextToPDF() throws IOException {
        Path textPath = tempDir.resolve("test.txt");
        Files.writeString(textPath, "Hello World from text file\nThis is a test.");
        
        Map<String, Object> result = pdfWorker.process("text-to-pdf", textPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testRepairPDF() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> result = pdfWorker.process("repair-pdf", pdfPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testOptimizePDF() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> result = pdfWorker.process("optimize", pdfPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testRemoveMetadata() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> params = new HashMap<>();
        params.put("clearAll", true);
        
        Map<String, Object> result = pdfWorker.process("metadata-edit", pdfPath, params, jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testRotatePDF() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> params = new HashMap<>();
        params.put("angle", 90);
        
        Map<String, Object> result = pdfWorker.process("rotate", pdfPath, params, jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testCompressPDF() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> params = new HashMap<>();
        params.put("level", "medium");
        
        Map<String, Object> result = pdfWorker.process("compress", pdfPath, params, jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testEncryptPDF() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> params = new HashMap<>();
        params.put("userPassword", "user123");
        params.put("ownerPassword", "owner456");
        
        Map<String, Object> result = pdfWorker.process("encrypt", pdfPath, params, jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testFlattenPDF() throws IOException {
        Path pdfPath = createTestPDF();
        
        Map<String, Object> result = pdfWorker.process("flatten-pdf", pdfPath, new HashMap<>(), jobStatus);
        
        assertNotNull(result);
        assertTrue(result.containsKey("resultUrl"));
    }
    
    @Test
    void testInvalidTool() {
        Path pdfPath = tempDir.resolve("test.pdf");
        
        assertThrows(PDFProcessingException.class, () -> {
            pdfWorker.process("invalid-tool-name", pdfPath, new HashMap<>(), jobStatus);
        });
    }
    
    @Test
    void testNullInput() {
        assertThrows(Exception.class, () -> {
            pdfWorker.process("extract-text", null, new HashMap<>(), jobStatus);
        });
    }
    
    private Path createTestPDF() throws IOException {
        Path pdfPath = tempDir.resolve("test_" + System.currentTimeMillis() + ".pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            // Add text content to the page
            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = 
                    new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(
                    new org.apache.pdfbox.pdmodel.font.PDType1Font(
                        org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Hello World! This is a test PDF document.");
                contentStream.endText();
            }
            
            document.save(pdfPath.toFile());
        }
        return pdfPath;
    }
}
