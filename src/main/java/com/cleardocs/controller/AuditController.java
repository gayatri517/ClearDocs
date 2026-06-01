package com.cleardocs.controller;

import com.cleardocs.model.mongo.AuditLog;
import com.cleardocs.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
@Tag(name = "Audit", description = "Audit trail and compliance log access")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Get all audit logs (paginated)")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @PageableDefault(size = 50, sort = "timestamp") Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogs(pageable));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get audit logs for a specific user")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
            @PathVariable Long userId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByUser(userId, pageable));
    }

    @GetMapping("/resources/{resourceType}/{resourceId}")
    @Operation(summary = "Get audit logs for a specific resource")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByResource(
            @PathVariable String resourceType,
            @PathVariable String resourceId,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByResource(resourceType, resourceId, pageable));
    }

    @GetMapping("/documents/{documentId}/trail")
    @Operation(summary = "Get full audit trail for a document")
    public ResponseEntity<List<AuditLog>> getDocumentAuditTrail(
            @PathVariable Long documentId,
            @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok(auditService.getDocumentAuditTrail(documentId, pageable));
    }

    @GetMapping("/range")
    @Operation(summary = "Get audit logs within a time range")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogsByTimeRange(from, to, pageable));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get audit summary statistics")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(auditService.getAuditSummary());
    }
}
