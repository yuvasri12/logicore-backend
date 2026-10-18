package com.devbridge.logicore.dto;

import java.util.List;

public class AuditPageResponse {

    private final List<AuditLogResponse> items;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public AuditPageResponse(List<AuditLogResponse> items, int page, int size,
                             long totalElements, int totalPages) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<AuditLogResponse> getItems() { return items; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}