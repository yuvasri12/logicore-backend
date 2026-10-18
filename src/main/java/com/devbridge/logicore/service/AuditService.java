package com.devbridge.logicore.service;

import com.devbridge.logicore.dto.AuditLogResponse;
import com.devbridge.logicore.dto.AuditPageResponse;
import com.devbridge.logicore.enums.AuditAction;
import com.devbridge.logicore.model.AuditLog;
import com.devbridge.logicore.repository.AuditLogRepository;
import com.devbridge.logicore.security.CurrentUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String SYSTEM_ACTOR = "system";
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Joins the caller's transaction, so the audit row and the business change
    // are saved together or rolled back together.
    @Transactional
    public void record(AuditAction action, String entityType, Object entityId, String detail) {
        String actor = CurrentUser.nameOr(SYSTEM_ACTOR);
        String id = String.valueOf(entityId);
        String safeDetail = detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail;
        auditLogRepository.save(new AuditLog(action, entityType, id, actor, safeDetail));
        log.info("AUDIT action={} entity={}#{} by={}", action, entityType, id, actor);
    }

    /** Newest first. With both entityType and entityId, only that record's history. */
    @Transactional(readOnly = true)
    public AuditPageResponse list(int page, int size, String entityType, String entityId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<AuditLog> result;
        if (hasText(entityType) && hasText(entityId)) {
            result = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                    entityType.trim().toUpperCase(), entityId.trim(), pageable);
        } else {
            result = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<AuditLogResponse> items = new ArrayList<AuditLogResponse>();
        for (AuditLog row : result.getContent()) {
            items.add(new AuditLogResponse(row.getId(), row.getAction(), row.getEntityType(),
                    row.getEntityId(), row.getPerformedBy(), row.getDetail(), row.getCreatedAt()));
        }
        return new AuditPageResponse(items, safePage, safeSize,
                result.getTotalElements(), result.getTotalPages());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}