package com.chnindia.eighteenpluspdf.exception;

public class PDFProcessingException extends RuntimeException {
    private final String errorCode;
    private final Object details;

    public PDFProcessingException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public PDFProcessingException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}