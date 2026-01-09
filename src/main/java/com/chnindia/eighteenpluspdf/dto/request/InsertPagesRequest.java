package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO for inserting pages from one PDF into another.
 */
public class InsertPagesRequest {
    
    @NotNull(message = "Target PDF file is required")
    private MultipartFile file;
    
    @NotNull(message = "Source PDF file is required")
    private MultipartFile sourcePdf;
    
    @NotNull(message = "Insert position is required")
    private Integer insertAfterPage;
    
    private String sourcePageRange = "all";
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    
    public MultipartFile getSourcePdf() { return sourcePdf; }
    public void setSourcePdf(MultipartFile sourcePdf) { this.sourcePdf = sourcePdf; }
    
    public Integer getInsertAfterPage() { return insertAfterPage; }
    public void setInsertAfterPage(Integer insertAfterPage) { this.insertAfterPage = insertAfterPage; }
    
    public String getSourcePageRange() { return sourcePageRange; }
    public void setSourcePageRange(String sourcePageRange) { this.sourcePageRange = sourcePageRange; }
    
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}
