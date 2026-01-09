package com.chnindia.eighteenpluspdf.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public class JobRequest {
    @NotBlank(message = "Tool name is required")
    private String toolName;
    
    @NotNull(message = "File is required")
    private MultipartFile file;
    
    private Map<String, Object> parameters;
    
    private String apiKey;

    // Getters and Setters
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }

    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}