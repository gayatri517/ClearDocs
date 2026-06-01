package com.cleardocs.service;

import com.cleardocs.model.mongo.AuditLog;
import com.cleardocs.repository.mongo.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    public Page<AuditLog> getAuditLogsByResource(String resourceType, String resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId, pageable);
    }

    public Page<AuditLog> getAuditLogsByTimeRange(Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(from, to, pageable);
    }

    public List<AuditLog> getDocumentAuditTrail(Long documentId, Pageable pageable) {
        return auditLogRepository.findDocumentAuditTrail(documentId.toString(), pageable);
    }

    public java.util.Map<String, Object> getAuditSummary() {
        long total = auditLogRepository.count();
        java.util.Map<String, Object> summary = new java.util.LinkedHashMap<>();
        summary.put("totalLogs", total);
        summary.put("generatedAt", Instant.now());
        return summary;
    }
}
