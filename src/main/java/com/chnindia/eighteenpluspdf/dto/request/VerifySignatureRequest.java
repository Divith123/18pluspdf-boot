package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for verifying digital signatures in a PDF document.
 */
public class VerifySignatureRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private Boolean verifyTimestamp = true;
    private Boolean checkCertificateValidity = true;
    private Boolean checkRevocation = false;
    private MultipartFile trustedCertificates;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    
    public Boolean getVerifyTimestamp() { return verifyTimestamp; }
    public void setVerifyTimestamp(Boolean verifyTimestamp) { this.verifyTimestamp = verifyTimestamp; }
    
    public Boolean getCheckCertificateValidity() { return checkCertificateValidity; }
    public void setCheckCertificateValidity(Boolean checkCertificateValidity) { this.checkCertificateValidity = checkCertificateValidity; }
    
    public Boolean getCheckRevocation() { return checkRevocation; }
    public void setCheckRevocation(Boolean checkRevocation) { this.checkRevocation = checkRevocation; }
    
    public MultipartFile getTrustedCertificates() { return trustedCertificates; }
    public void setTrustedCertificates(MultipartFile trustedCertificates) { this.trustedCertificates = trustedCertificates; }
}
