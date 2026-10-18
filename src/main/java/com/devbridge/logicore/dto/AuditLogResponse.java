package com.devbridge.logicore.dto;

import com.devbridge.logicore.enums.AuditAction;

import java.time.Instant;

public class AuditLogResponse {

    private final Long id;
    private final AuditAction action;
    private final String entityType;
    private final String entityId;
    private final String performedBy;
    private final String detail;
    private final Instant createdAt;

    public AuditLogResponse(Long id, AuditAction action, String entityType, String entityId,
                            String performedBy, String detail, Instant createdAt) {
        this.id = id;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.performedBy = performedBy;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public AuditAction getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getPerformedBy() { return performedBy; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
}