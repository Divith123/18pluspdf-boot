package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO for reordering pages in a PDF document.
 */
public class ReorderPagesRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    @NotNull(message = "Page order is required")
    private List<Integer> pageOrder;
    
    private String outputFileName;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
    
    public List<Integer> getPageOrder() { return pageOrder; }
    public void setPageOrder(List<Integer> pageOrder) { this.pageOrder = pageOrder; }
    
    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }
}
