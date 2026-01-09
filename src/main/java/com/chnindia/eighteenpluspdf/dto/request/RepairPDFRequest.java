package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for repairing a corrupted PDF document.
 */
public class RepairPDFRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private Boolean rebuildXref = true;
    private Boolean removeCorruptedObjects = true;
    private Boolean recoverPages = true;
    private Boolean fixStreamLengths = true;
    private Boolean validateAfterRepair = true;
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    
    public Boolean getRebuildXref() { return rebuildXref; }
    public void setRebuildXref(Boolean rebuildXref) { this.rebuildXref = rebuildXref; }
    
    public Boolean getRemoveCorruptedObjects() { return removeCorruptedObjects; }
    public void setRemoveCorruptedObjects(Boolean removeCorruptedObjects) { this.removeCorruptedObjects = removeCorruptedObjects; }
    
    public Boolean getRecoverPages() { return recoverPages; }
    public void setRecoverPages(Boolean recoverPages) { this.recoverPages = recoverPages; }
    
    public Boolean getFixStreamLengths() { return fixStreamLengths; }
    public void setFixStreamLengths(Boolean fixStreamLengths) { this.fixStreamLengths = fixStreamLengths; }
    
    public Boolean getValidateAfterRepair() { return validateAfterRepair; }
    public void setValidateAfterRepair(Boolean validateAfterRepair) { this.validateAfterRepair = validateAfterRepair; }
    
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}
