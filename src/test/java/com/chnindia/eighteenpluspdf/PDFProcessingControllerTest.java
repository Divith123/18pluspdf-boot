package com.chnindia.eighteenpluspdf;

import com.chnindia.eighteenpluspdf.controller.PDFProcessingController;
import com.chnindia.eighteenpluspdf.dto.JobRequest;
import com.chnindia.eighteenpluspdf.dto.JobResponse;
import com.chnindia.eighteenpluspdf.dto.response.JobStatusResponse;
import com.chnindia.eighteenpluspdf.model.JobStatus;
import com.chnindia.eighteenpluspdf.service.JobQueueService;
import com.chnindia.eighteenpluspdf.util.FileUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PDFProcessingControllerTest {
    
    @Mock
    private JobQueueService jobQueueService;
    
    @Mock
    private FileUtil fileUtil;
    
    @InjectMocks
    private PDFProcessingController controller;
    
    private MockMultipartFile testFile;
    
    @BeforeEach
    void setUp() {
        testFile = new MockMultipartFile("file", "test.pdf", 
            "application/pdf", "test content".getBytes());
    }
    
    @Test
    void testMergePDFs() {
        JobResponse response = new JobResponse();
        response.setJobId("test-job-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        MockMultipartFile file1 = new MockMultipartFile("files", "test1.pdf", 
            "application/pdf", "test content".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.pdf", 
            "application/pdf", "test content".getBytes());
        
        // mergePDFs(MultipartFile[] files, String outputFileName, Boolean preserveBookmarks, Boolean removeAnnotations)
        ResponseEntity<?> result = controller.mergePDFs(
            new MockMultipartFile[]{file1, file2}, "merged.pdf", true, false);
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testGetJobStatus() {
        JobStatusResponse response = new JobStatusResponse();
        response.setJobId("test-job-123");
        response.setStatus(JobStatus.Status.COMPLETED);
        
        when(jobQueueService.getJobStatus("test-job-123")).thenReturn(response);
        
        ResponseEntity<?> result = controller.getJobStatus("test-job-123");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testCompressPDF() {
        JobResponse response = new JobResponse();
        response.setJobId("compress-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // compressPDF(MultipartFile file, Double compressionQuality, Boolean removeMetadata, Boolean optimizeImages, Integer maxImageDpi, String outputFileName)
        ResponseEntity<?> result = controller.compressPDF(
            testFile, 0.75, false, true, 150, "compressed.pdf");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testExtractText() {
        JobResponse response = new JobResponse();
        response.setJobId("extract-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // extractText(MultipartFile file)
        ResponseEntity<?> result = controller.extractText(testFile);
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testPDFToImage() {
        JobResponse response = new JobResponse();
        response.setJobId("pdf2img-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // pdfToImage(MultipartFile file, String imageFormat, Integer dpi, String pageRange, String outputPrefix)
        ResponseEntity<?> result = controller.pdfToImage(testFile, "png", 300, "all", "output");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testOCRExtract() {
        JobResponse response = new JobResponse();
        response.setJobId("ocr-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // ocrPDF(MultipartFile file, String language, Integer dpi, String outputFileName, Boolean makeSearchable, Boolean preserveOriginal)
        ResponseEntity<?> result = controller.ocrPDF(testFile, "eng", 300, "ocr_output.pdf", true, false);
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testComparePDFs() {
        JobResponse response = new JobResponse();
        response.setJobId("compare-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // comparePDFs(MultipartFile file1, MultipartFile file2, String outputFileName, Boolean compareText, Boolean compareImages, Boolean compareLayout, Double tolerance)
        ResponseEntity<?> result = controller.comparePDFs(testFile, testFile, "compare_output.pdf", true, true, true, 0.1);
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testCancelJob() {
        when(jobQueueService.cancelJob(anyString())).thenReturn(true);
        
        ResponseEntity<?> result = controller.cancelJob("test-job-123");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testRotatePDF() {
        JobResponse response = new JobResponse();
        response.setJobId("rotate-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // rotatePDF(MultipartFile file, Integer angle, String pageRange, String outputFileName)
        ResponseEntity<?> result = controller.rotatePDF(testFile, 90, "all", "rotated.pdf");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testEncryptPDF() {
        JobResponse response = new JobResponse();
        response.setJobId("encrypt-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // encryptPDF(MultipartFile file, String ownerPassword, String userPassword, Boolean allowPrint, Boolean allowCopy, Boolean allowModify, String outputFileName)
        ResponseEntity<?> result = controller.encryptPDF(
            testFile, "owner123", "user123", true, true, true, "encrypted.pdf");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testDecryptPDF() {
        JobResponse response = new JobResponse();
        response.setJobId("decrypt-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // decryptPDF(MultipartFile file, String password, String outputFileName)
        ResponseEntity<?> result = controller.decryptPDF(testFile, "password123", "decrypted.pdf");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
    
    @Test
    void testWatermarkPDF() {
        JobResponse response = new JobResponse();
        response.setJobId("watermark-123");
        response.setStatus(JobStatus.Status.PENDING);
        
        when(jobQueueService.submitJob(any(JobRequest.class))).thenReturn(response);
        
        // addWatermark(MultipartFile file, String watermarkText, String fontName, Integer fontSize, String color, Double opacity, String position, Integer rotation, Boolean diagonal, String outputFileName)
        ResponseEntity<?> result = controller.addWatermark(
            testFile, "CONFIDENTIAL", "Helvetica", 48, "#808080", 0.3, "center", 45, true, "watermarked.pdf");
        
        assertTrue(result.getStatusCode().is2xxSuccessful());
    }
}
