package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for encrypting PDF files
 * API Endpoint: POST /api/pdf/encrypt
 */
public class EncryptPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @NotBlank(message = "Owner password is required")
    private String ownerPassword;
    
    private String userPassword;
    
    private Boolean allowPrint = true;
    
    private Boolean allowCopy = true;
    
    private Boolean allowModify = true;
    
    private Boolean allowAnnotate = true;
    
    private Boolean allowFillForms = true;
    
    private Boolean allowAccessibility = true;
    
    private Boolean allowAssemble = true;
    
    private Boolean allowDegradedPrint = true;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOwnerPassword() { return ownerPassword; }
    public void setOwnerPassword(String ownerPassword) { this.ownerPassword = ownerPassword; }

    public String getUserPassword() { return userPassword; }
    public void setUserPassword(String userPassword) { this.userPassword = userPassword; }

    public Boolean getAllowPrint() { return allowPrint; }
    public void setAllowPrint(Boolean allowPrint) { this.allowPrint = allowPrint; }

    public Boolean getAllowCopy() { return allowCopy; }
    public void setAllowCopy(Boolean allowCopy) { this.allowCopy = allowCopy; }

    public Boolean getAllowModify() { return allowModify; }
    public void setAllowModify(Boolean allowModify) { this.allowModify = allowModify; }

    public Boolean getAllowAnnotate() { return allowAnnotate; }
    public void setAllowAnnotate(Boolean allowAnnotate) { this.allowAnnotate = allowAnnotate; }

    public Boolean getAllowFillForms() { return allowFillForms; }
    public void setAllowFillForms(Boolean allowFillForms) { this.allowFillForms = allowFillForms; }

    public Boolean getAllowAccessibility() { return allowAccessibility; }
    public void setAllowAccessibility(Boolean allowAccessibility) { this.allowAccessibility = allowAccessibility; }

    public Boolean getAllowAssemble() { return allowAssemble; }
    public void setAllowAssemble(Boolean allowAssemble) { this.allowAssemble = allowAssemble; }

    public Boolean getAllowDegradedPrint() { return allowDegradedPrint; }
    public void setAllowDegradedPrint(Boolean allowDegradedPrint) { this.allowDegradedPrint = allowDegradedPrint; }
}