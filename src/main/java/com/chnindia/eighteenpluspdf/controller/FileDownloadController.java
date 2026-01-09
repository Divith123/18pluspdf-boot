package com.chnindia.eighteenpluspdf.controller;

import com.chnindia.eighteenpluspdf.util.FileUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/pdf")
@Tag(name = "File Download", description = "Download processed PDF files")
public class FileDownloadController {
    
    @Autowired
    private FileUtil fileUtil;
    
    @GetMapping("/download/{filename}")
    @Operation(summary = "Download file", description = "Download a processed PDF file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "File not found"),
        @ApiResponse(responseCode = "400", description = "Invalid filename")
    })
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Filename to download", required = true)
            @PathVariable String filename) {
        
        try {
            // Sanitize filename to prevent path traversal
            if (!fileUtil.isSafePath(filename)) {
                return ResponseEntity.badRequest().build();
            }
            
            Path file = Paths.get("./data/output").resolve(filename).normalize();
            
            if (!Files.exists(file)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(file.toUri());
            
            String contentType = Files.probeContentType(file);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
            
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/download/{jobId}/{filename}")
    @Operation(summary = "Download job file", description = "Download a file from a specific job")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
        @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Resource> downloadJobFile(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId,
            @Parameter(description = "Filename", required = true)
            @PathVariable String filename) {
        
        // For now, just use the filename
        // In production, you might want to verify the job belongs to the user
        return downloadFile(filename);
    }
}