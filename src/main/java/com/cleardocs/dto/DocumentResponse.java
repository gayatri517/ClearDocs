package com.cleardocs.dto;

import com.cleardocs.statemachine.DocumentState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private String referenceNumber;
    private String title;
    private String description;
    private String documentType;
    private String priority;
    private DocumentState state;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Long submitterId;
    private String submitterName;
    private String submitterEmail;
    private Instant submittedAt;
    private Instant approvedAt;
    private Instant rejectedAt;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
