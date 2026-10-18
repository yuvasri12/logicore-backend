package com.devbridge.logicore.dto;

import java.time.Instant;
import java.util.List;

public class ApiErrorResponse {

    private final Instant timestamp = Instant.now();
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final List<String> details;
    private final String path;
    private final String requestId;

    public ApiErrorResponse(int status, String error, String code, String message,
                            List<String> details, String path, String requestId) {
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.details = details;
        this.path = path;
        this.requestId = requestId;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public List<String> getDetails() { return details; }
    public String getPath() { return path; }
    public String getRequestId() { return requestId; }
}