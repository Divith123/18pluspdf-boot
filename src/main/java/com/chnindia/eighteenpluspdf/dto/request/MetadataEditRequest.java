package com.chnindia.eighteenpluspdf.dto.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Request DTO for editing PDF metadata
 * API Endpoint: POST /api/pdf/metadata-edit
 */
public class MetadataEditRequest {
    
    @NotNull(message = "PDF file is required")
    private MultipartFile file;
    
    private String outputFileName;
    
    private String title;
    
    private String author;
    
    private String subject;
    
    private String keywords;
    
    private String creator;
    
    private String producer;
    
    private Map<String, String> customMetadata;

    // Getters and Setters
    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public String getOutputFileName() { return outputFileName; }
    public void setOutputFileName(String outputFileName) { this.outputFileName = outputFileName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public String getProducer() { return producer; }
    public void setProducer(String producer) { this.producer = producer; }

    public Map<String, String> getCustomMetadata() { return customMetadata; }
    public void setCustomMetadata(Map<String, String> customMetadata) { this.customMetadata = customMetadata; }
}