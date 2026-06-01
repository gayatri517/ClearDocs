package com.cleardocs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowStepResponse {
    private Long id;
    private Integer stepOrder;
    private Long reviewerId;
    private String reviewerName;
    private String reviewerRole;
    private String status;
    private String comments;
    private String actionTaken;
    private Instant dueDate;
    private Instant actedAt;
}
