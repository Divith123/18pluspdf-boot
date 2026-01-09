package com.chnindia.eighteenpluspdf.exception;

public class JobNotFoundException extends RuntimeException {
    private final String errorCode;

    public JobNotFoundException(String jobId) {
        super("Job not found: " + jobId);
        this.errorCode = "JOB_NOT_FOUND";
    }

    public String getErrorCode() {
        return errorCode;
    }
}