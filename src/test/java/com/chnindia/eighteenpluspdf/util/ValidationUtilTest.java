package com.chnindia.eighteenpluspdf.util;

import com.chnindia.eighteenpluspdf.exception.PDFProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationUtilTest {
    
    @Test
    void testValidateFileNotNull_WithValidFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        
        assertDoesNotThrow(() -> ValidationUtil.validateFileNotNull(file, "testFile"));
    }
    
    @Test
    void testValidateFileNotNull_WithNullFile() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateFileNotNull(null, "testFile"));
    }
    
    @Test
    void testValidateFileNotNull_WithEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);
        
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateFileNotNull(file, "testFile"));
    }
    
    @Test
    void testValidateRotationAngle_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateRotationAngle(90));
        assertDoesNotThrow(() -> ValidationUtil.validateRotationAngle(180));
        assertDoesNotThrow(() -> ValidationUtil.validateRotationAngle(270));
    }
    
    @Test
    void testValidateRotationAngle_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateRotationAngle(45));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateRotationAngle(360));
    }
    
    @Test
    void testValidateColor_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateColor("#FF0000"));
        assertDoesNotThrow(() -> ValidationUtil.validateColor("#000000"));
        assertDoesNotThrow(() -> ValidationUtil.validateColor("#ABCDEF"));
    }
    
    @Test
    void testValidateColor_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateColor("red"));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateColor("#GGGGGG"));
    }
    
    @Test
    void testValidateOpacity_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateOpacity(0.0));
        assertDoesNotThrow(() -> ValidationUtil.validateOpacity(0.5));
        assertDoesNotThrow(() -> ValidationUtil.validateOpacity(1.0));
    }
    
    @Test
    void testValidateOpacity_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateOpacity(-0.1));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateOpacity(1.1));
    }
    
    @Test
    void testValidatePositiveInteger_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validatePositiveInteger(1, "test"));
        assertDoesNotThrow(() -> ValidationUtil.validatePositiveInteger(100, "test"));
    }
    
    @Test
    void testValidatePositiveInteger_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePositiveInteger(0, "test"));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePositiveInteger(-1, "test"));
    }
    
    @Test
    void testValidateDPI_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateDPI(72));
        assertDoesNotThrow(() -> ValidationUtil.validateDPI(300));
        assertDoesNotThrow(() -> ValidationUtil.validateDPI(600));
    }
    
    @Test
    void testValidateDPI_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateDPI(50));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateDPI(700));
    }
    
    @Test
    void testValidatePasswordStrength_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validatePasswordStrength("password123"));
        assertDoesNotThrow(() -> ValidationUtil.validatePasswordStrength("longpassword"));
    }
    
    @Test
    void testValidatePasswordStrength_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePasswordStrength("short"));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePasswordStrength("12345"));
    }
    
    @Test
    void testValidateOCRLanguage_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateOCRLanguage("eng"));
        assertDoesNotThrow(() -> ValidationUtil.validateOCRLanguage("spa"));
        assertDoesNotThrow(() -> ValidationUtil.validateOCRLanguage("fra"));
    }
    
    @Test
    void testValidateOCRLanguage_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateOCRLanguage("english"));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateOCRLanguage("en"));
    }
    
    @Test
    void testValidateOutputFilename_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateOutputFilename("file.pdf"));
        assertDoesNotThrow(() -> ValidationUtil.validateOutputFilename("my_file-123.pdf"));
        assertDoesNotThrow(() -> ValidationUtil.validateOutputFilename(null)); // null is allowed
    }
    
    @Test
    void testValidateOutputFilename_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateOutputFilename("../file.pdf"));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateOutputFilename("file/name.pdf"));
    }
    
    @Test
    void testValidateCompressionQuality_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateCompressionQuality(0.1));
        assertDoesNotThrow(() -> ValidationUtil.validateCompressionQuality(0.5));
        assertDoesNotThrow(() -> ValidationUtil.validateCompressionQuality(1.0));
    }
    
    @Test
    void testValidateCompressionQuality_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateCompressionQuality(0.05));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateCompressionQuality(1.1));
    }
    
    @Test
    void testValidateMargin_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateMargin(0));
        assertDoesNotThrow(() -> ValidationUtil.validateMargin(36));
        assertDoesNotThrow(() -> ValidationUtil.validateMargin(100));
    }
    
    @Test
    void testValidateMargin_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateMargin(-1));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateMargin(101));
    }
    
    @Test
    void testValidateFontSize_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateFontSize(6));
        assertDoesNotThrow(() -> ValidationUtil.validateFontSize(12));
        assertDoesNotThrow(() -> ValidationUtil.validateFontSize(72));
    }
    
    @Test
    void testValidateFontSize_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateFontSize(5));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateFontSize(73));
    }
    
    @Test
    void testValidatePageSize_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validatePageSize("A4"));
        assertDoesNotThrow(() -> ValidationUtil.validatePageSize("LETTER"));
        assertDoesNotThrow(() -> ValidationUtil.validatePageSize(null)); // null is allowed
    }
    
    @Test
    void testValidatePageSize_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePageSize("INVALID"));
    }
    
    @Test
    void testValidateComplianceLevel_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validateComplianceLevel("1B"));
        assertDoesNotThrow(() -> ValidationUtil.validateComplianceLevel("2B"));
        assertDoesNotThrow(() -> ValidationUtil.validateComplianceLevel("2U"));
        assertDoesNotThrow(() -> ValidationUtil.validateComplianceLevel("3B"));
    }
    
    @Test
    void testValidateComplianceLevel_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateComplianceLevel("4B"));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validateComplianceLevel("1A"));
    }
    
    @Test
    void testGetFileExtension() {
        assertEquals("pdf", ValidationUtil.getFileExtension("document.pdf"));
        assertEquals("txt", ValidationUtil.getFileExtension("file.txt"));
        assertEquals("", ValidationUtil.getFileExtension("file"));
        assertEquals("", ValidationUtil.getFileExtension(null));
    }
    
    @Test
    void testGetBaseFilename() {
        assertEquals("document", ValidationUtil.getBaseFilename("document.pdf"));
        assertEquals("file", ValidationUtil.getBaseFilename("file.txt"));
        assertEquals("file", ValidationUtil.getBaseFilename("file"));
        assertEquals("", ValidationUtil.getBaseFilename(null));
    }
    
    @Test
    void testValidatePageRange_Valid() {
        assertDoesNotThrow(() -> ValidationUtil.validatePageRange("all", 10));
        assertDoesNotThrow(() -> ValidationUtil.validatePageRange("1-5", 10));
        assertDoesNotThrow(() -> ValidationUtil.validatePageRange("1,3,5", 10));
        assertDoesNotThrow(() -> ValidationUtil.validatePageRange("1-3,5,7-9", 10));
    }
    
    @Test
    void testValidatePageRange_Invalid() {
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePageRange("1-15", 10));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePageRange("0-5", 10));
        assertThrows(PDFProcessingException.class, 
            () -> ValidationUtil.validatePageRange("invalid", 10));
    }
}