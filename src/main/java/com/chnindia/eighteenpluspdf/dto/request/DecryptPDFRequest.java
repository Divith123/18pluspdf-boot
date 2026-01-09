package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for decrypting PDF files
 * API Endpoint: POST /api/pdf/decrypt
 */
public class DecryptPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @NotBlank(message = "Password is required")
    private String password;
    
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}