package com.devbridge.logicore.controller;

import com.devbridge.logicore.dto.AuditPageResponse;
import com.devbridge.logicore.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public AuditPageResponse list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String entityType,
                                  @RequestParam(required = false) String entityId) {
        return auditService.list(page, size, entityType, entityId);
    }
}