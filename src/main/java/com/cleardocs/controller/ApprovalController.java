package com.cleardocs.controller;

import com.cleardocs.dto.ApprovalRequest;
import com.cleardocs.dto.DocumentResponse;
import com.cleardocs.dto.WorkflowStepResponse;
import com.cleardocs.security.UserPrincipal;
import com.cleardocs.service.ApprovalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/approvals")
@RequiredArgsConstructor
@Tag(name = "Approvals", description = "Document approval workflow management")
@SecurityRequirement(name = "bearerAuth")
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping("/documents/{documentId}/start-review")
    @PreAuthorize("hasAnyRole('REVIEWER','APPROVER','ADMIN')")
    @Operation(summary = "Start review process for a submitted document")
    public ResponseEntity<DocumentResponse> startReview(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.startReview(documentId, principal));
    }

    @PostMapping("/documents/{documentId}/action")
    @PreAuthorize("hasAnyRole('REVIEWER','APPROVER','ADMIN')")
    @Operation(summary = "Perform a review action: APPROVE, REJECT, or REQUEST_REVISION")
    public ResponseEntity<DocumentResponse> processAction(
            @PathVariable Long documentId,
            @Valid @RequestBody ApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.processReviewAction(documentId, request, principal));
    }

    @GetMapping("/documents/{documentId}/steps")
    @Operation(summary = "Get all workflow steps for a document")
    public ResponseEntity<List<WorkflowStepResponse>> getWorkflowSteps(@PathVariable Long documentId) {
        return ResponseEntity.ok(approvalService.getWorkflowSteps(documentId));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('REVIEWER','APPROVER','ADMIN')")
    @Operation(summary = "Get all documents pending review for the current user")
    public ResponseEntity<List<DocumentResponse>> getPendingReviews(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(approvalService.getPendingForReviewer(principal));
    }
}
