package com.chnindia.eighteenpluspdf.dto.response;

import java.time.LocalDateTime;

/**
 * Standard error response DTO for API error handling.
 * Provides consistent error format across all endpoints.
 */
public class ErrorResponse {
    private final LocalDateTime timestamp;
    private final String errorCode;
    private final String message;
    private final Object details;

    public ErrorResponse(LocalDateTime timestamp, String errorCode, String message, Object details) {
        this.timestamp = timestamp;
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    public ErrorResponse(String errorCode, String message) {
        this(LocalDateTime.now(), errorCode, message, null);
    }

    public ErrorResponse(String errorCode, String message, Object details) {
        this(LocalDateTime.now(), errorCode, message, details);
    }

    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public Object getDetails() { return details; }
}
