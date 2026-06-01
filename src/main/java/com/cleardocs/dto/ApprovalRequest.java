package com.cleardocs.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ApprovalRequest {
    @NotBlank
    private String action; // APPROVE, REJECT, REQUEST_REVISION

    private String comments;

    private String rejectionReason;
}
