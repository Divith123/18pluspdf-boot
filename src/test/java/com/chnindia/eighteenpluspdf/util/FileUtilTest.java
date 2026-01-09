package com.chnindia.eighteenpluspdf.util;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {
    
    @TempDir
    Path tempDir;
    
    private FileUtil fileUtil;
    
    @BeforeEach
    void setUp() throws Exception {
        fileUtil = new FileUtil();
        // Use reflection to set the private fields
        var tempDirField = FileUtil.class.getDeclaredField("tempDir");
        tempDirField.setAccessible(true);
        tempDirField.set(fileUtil, tempDir.toString());
        
        var outputDirField = FileUtil.class.getDeclaredField("outputDir");
        outputDirField.setAccessible(true);
        outputDirField.set(fileUtil, tempDir.resolve("output").toString());
        
        var allowedExtensionsField = FileUtil.class.getDeclaredField("allowedExtensions");
        allowedExtensionsField.setAccessible(true);
        allowedExtensionsField.set(fileUtil, ".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.jpg,.jpeg,.png,.bmp,.tiff,.txt");
        
        // Create output directory
        Files.createDirectories(tempDir.resolve("output"));
    }
    
    @Test
    void testValidateFile_ValidFile() {
        MultipartFile validFile = new MockMultipartFile(
            "file", 
            "test.pdf", 
            "application/pdf", 
            "test content".getBytes()
        );
        assertDoesNotThrow(() -> FileUtil.validateFile(validFile));
    }
    
    @Test
    void testValidateFile_NullFile() {
        assertThrows(PDFProcessingException.class, () -> FileUtil.validateFile(null));
    }
    
    @Test
    void testValidateFile_EmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile(
            "file", 
            "empty.pdf", 
            "application/pdf", 
            new byte[0]
        );
        assertThrows(PDFProcessingException.class, () -> FileUtil.validateFile(emptyFile));
    }
    
    @Test
    void testValidateFile_UnsupportedFormat() {
        MultipartFile unsupportedFile = new MockMultipartFile(
            "file", 
            "test.exe", 
            "application/x-msdownload", 
            "test content".getBytes()
        );
        assertThrows(PDFProcessingException.class, () -> FileUtil.validateFile(unsupportedFile));
    }
    
    @Test
    void testSaveTempFile() throws IOException {
        MultipartFile file = new MockMultipartFile(
            "file", 
            "test.pdf", 
            "application/pdf", 
            "test content".getBytes()
        );
        
        Path savedPath = fileUtil.saveTempFile(file);
        
        assertNotNull(savedPath);
        assertTrue(Files.exists(savedPath));
        assertTrue(savedPath.toString().endsWith(".pdf"));
        assertEquals("test content", Files.readString(savedPath));
    }
    
    @Test
    void testCreateTempDirectory() throws IOException {
        Path dirPath = fileUtil.createTempDirectory();
        
        assertNotNull(dirPath);
        assertTrue(Files.exists(dirPath));
        assertTrue(Files.isDirectory(dirPath));
    }
    
    @Test
    void testCalculateFileHash() throws IOException {
        Path testFile = tempDir.resolve("hashtest.txt");
        Files.writeString(testFile, "test content");
        
        String hash1 = fileUtil.calculateFileHash(testFile);
        
        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 produces 64 hex chars
        
        // Same content should produce same hash
        Path testFile2 = tempDir.resolve("hashtest2.txt");
        Files.writeString(testFile2, "test content");
        String hash2 = fileUtil.calculateFileHash(testFile2);
        assertEquals(hash1, hash2);
        
        // Different content should produce different hash
        Path testFile3 = tempDir.resolve("hashtest3.txt");
        Files.writeString(testFile3, "different content");
        String hash3 = fileUtil.calculateFileHash(testFile3);
        assertNotEquals(hash1, hash3);
    }
    
    @Test
    void testIsValidExtension() {
        assertTrue(fileUtil.isValidExtension("test.pdf"));
        assertTrue(fileUtil.isValidExtension("test.PDF"));
        assertTrue(fileUtil.isValidExtension("test.doc"));
        assertTrue(fileUtil.isValidExtension("test.docx"));
        assertTrue(fileUtil.isValidExtension("test.jpg"));
        assertTrue(fileUtil.isValidExtension("test.png"));
        
        assertFalse(fileUtil.isValidExtension("test.exe"));
        assertFalse(fileUtil.isValidExtension("test.bat"));
        assertFalse(fileUtil.isValidExtension(null));
    }
    
    @Test
    void testIsSafePath() {
        assertTrue(fileUtil.isSafePath("document.pdf"));
        assertTrue(fileUtil.isSafePath("report_2024.pdf"));
        
        assertFalse(fileUtil.isSafePath("../../../etc/passwd"));
        assertFalse(fileUtil.isSafePath("..\\..\\windows\\system32"));
        assertFalse(fileUtil.isSafePath("path/with/slash.pdf"));
        assertFalse(fileUtil.isSafePath(null));
        assertFalse(fileUtil.isSafePath(""));
    }
    
    @Test
    void testGetFileType() {
        assertEquals("pdf", fileUtil.getFileType("document.pdf"));
        assertEquals("image", fileUtil.getFileType("photo.jpg"));
        assertEquals("image", fileUtil.getFileType("photo.PNG"));
        assertEquals("office", fileUtil.getFileType("doc.docx"));
        assertEquals("office", fileUtil.getFileType("sheet.xlsx"));
        assertEquals("text", fileUtil.getFileType("readme.txt"));
        assertEquals("text", fileUtil.getFileType("page.html"));
        assertEquals("unknown", fileUtil.getFileType("file.xyz"));
    }
    
    @Test
    void testCleanupTempFile() throws IOException {
        Path testFile = tempDir.resolve("cleanup_test.txt");
        Files.writeString(testFile, "content");
        assertTrue(Files.exists(testFile));
        
        fileUtil.cleanupTempFile(testFile);
        
        assertFalse(Files.exists(testFile));
    }
    
    @Test
    void testCleanupTempFile_NonExistent() {
        Path nonExistent = tempDir.resolve("does_not_exist.txt");
        // Should not throw
        assertDoesNotThrow(() -> fileUtil.cleanupTempFile(nonExistent));
    }
    
    @Test
    void testGetHumanReadableSize() throws IOException {
        Path testFile = tempDir.resolve("size_test.txt");
        Files.writeString(testFile, "A".repeat(500));
        
        String size = fileUtil.getHumanReadableSize(testFile);
        assertNotNull(size);
        assertTrue(size.contains("B"));
    }
    
    @Test
    void testIsPdfFile() {
        assertTrue(fileUtil.isPdfFile("document.pdf"));
        assertTrue(fileUtil.isPdfFile("document.PDF"));
        assertFalse(fileUtil.isPdfFile("document.doc"));
        assertFalse(fileUtil.isPdfFile("document.txt"));
    }
    
    @Test
    void testIsImageFile() {
        assertTrue(fileUtil.isImageFile("photo.jpg"));
        assertTrue(fileUtil.isImageFile("photo.jpeg"));
        assertTrue(fileUtil.isImageFile("photo.png"));
        assertTrue(fileUtil.isImageFile("photo.bmp"));
        assertFalse(fileUtil.isImageFile("document.pdf"));
    }
    
    @Test
    void testIsOfficeFile() {
        assertTrue(fileUtil.isOfficeFile("document.doc"));
        assertTrue(fileUtil.isOfficeFile("document.docx"));
        assertTrue(fileUtil.isOfficeFile("spreadsheet.xls"));
        assertTrue(fileUtil.isOfficeFile("spreadsheet.xlsx"));
        assertTrue(fileUtil.isOfficeFile("presentation.ppt"));
        assertTrue(fileUtil.isOfficeFile("presentation.pptx"));
        assertFalse(fileUtil.isOfficeFile("document.pdf"));
    }
    
    @Test
    void testGetBaseFilename() {
        assertEquals("test", fileUtil.getBaseFilename("test.pdf"));
        assertEquals("test.backup", fileUtil.getBaseFilename("test.backup.pdf"));
        assertEquals("test", fileUtil.getBaseFilename("test"));
    }
    
    @Test
    void testGetDownloadUrl() {
        String url = fileUtil.getDownloadUrl("document.pdf");
        assertEquals("/api/pdf/download/document.pdf", url);
    }
    
    @Test
    void testCleanupFileStatic() throws IOException {
        Path testFile = tempDir.resolve("static_cleanup.txt");
        Files.writeString(testFile, "content");
        assertTrue(Files.exists(testFile));
        
        FileUtil.cleanupFile(testFile);
        
        assertFalse(Files.exists(testFile));
    }
    
    @Test
    void testGetMimeType() throws IOException {
        Path pdfFile = tempDir.resolve("test.pdf");
        Files.writeString(pdfFile, "%PDF-1.4 test content");
        
        String mimeType = FileUtil.getMimeType(pdfFile);
        // Mime type detection depends on the system
        assertNotNull(mimeType);
    }
    
    @Test
    void testIsPDFStatic() throws IOException {
        Path pdfFile = tempDir.resolve("test.pdf");
        Files.writeString(pdfFile, "content");
        
        assertTrue(FileUtil.isPDF(pdfFile));
        
        Path txtFile = tempDir.resolve("test.txt");
        Files.writeString(txtFile, "content");
        
        assertFalse(FileUtil.isPDF(txtFile));
    }
    
    @Test
    void testCreateOutputFile() throws IOException {
        Path outputFile = fileUtil.createOutputFile("report", "pdf");
        
        assertNotNull(outputFile);
        assertTrue(outputFile.toString().endsWith(".pdf"));
        assertTrue(outputFile.toString().contains("report"));
    }
    
    @Test
    void testSaveMultipleTempFiles() throws IOException {
        MultipartFile file1 = new MockMultipartFile("file1", "test1.pdf", "application/pdf", "content1".getBytes());
        MultipartFile file2 = new MockMultipartFile("file2", "test2.pdf", "application/pdf", "content2".getBytes());
        
        var savedPaths = fileUtil.saveTempFiles(new MultipartFile[]{file1, file2});
        
        assertEquals(2, savedPaths.size());
        assertTrue(Files.exists(savedPaths.get(0)));
        assertTrue(Files.exists(savedPaths.get(1)));
    }
}
