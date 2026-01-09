package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for digitally signing a PDF document.
 */
public class SignPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @NotNull(message = "Certificate file is required")
    private MultipartFile certificate;
    
    @NotNull(message = "Certificate password is required")
    private String certificatePassword;
    
    private String reason;
    private String location;
    private String contact;
    private String signatureField;
    private Integer signaturePage;
    private Float signatureX;
    private Float signatureY;
    private Float signatureWidth;
    private Float signatureHeight;
    private Boolean visibleSignature = true;
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    
    public MultipartFile getCertificate() { return certificate; }
    public void setCertificate(MultipartFile certificate) { this.certificate = certificate; }
    
    public String getCertificatePassword() { return certificatePassword; }
    public void setCertificatePassword(String certificatePassword) { this.certificatePassword = certificatePassword; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    
    public String getSignatureField() { return signatureField; }
    public void setSignatureField(String signatureField) { this.signatureField = signatureField; }
    
    public Integer getSignaturePage() { return signaturePage; }
    public void setSignaturePage(Integer signaturePage) { this.signaturePage = signaturePage; }
    
    public Float getSignatureX() { return signatureX; }
    public void setSignatureX(Float signatureX) { this.signatureX = signatureX; }
    
    public Float getSignatureY() { return signatureY; }
    public void setSignatureY(Float signatureY) { this.signatureY = signatureY; }
    
    public Float getSignatureWidth() { return signatureWidth; }
    public void setSignatureWidth(Float signatureWidth) { this.signatureWidth = signatureWidth; }
    
    public Float getSignatureHeight() { return signatureHeight; }
    public void setSignatureHeight(Float signatureHeight) { this.signatureHeight = signatureHeight; }
    
    public Boolean getVisibleSignature() { return visibleSignature; }
    public void setVisibleSignature(Boolean visibleSignature) { this.visibleSignature = visibleSignature; }
    
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}
