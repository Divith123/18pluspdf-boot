package com.chnindia.eighteenpluspdf.exception;

public class ExternalToolException extends RuntimeException {
    private final String errorCode;
    private final Object details;

    public ExternalToolException(String errorCode, String message, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public ExternalToolException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getDetails() {
        return details;
    }
}