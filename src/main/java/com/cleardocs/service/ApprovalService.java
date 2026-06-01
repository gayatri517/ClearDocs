package com.cleardocs.service;

import com.cleardocs.aop.AuditAspect.Audited;
import com.cleardocs.dto.ApprovalRequest;
import com.cleardocs.dto.DocumentResponse;
import com.cleardocs.dto.WorkflowStepResponse;
import com.cleardocs.exception.DocumentNotFoundException;
import com.cleardocs.model.jpa.ApprovalWorkflow;
import com.cleardocs.model.jpa.Document;
import com.cleardocs.model.jpa.WorkflowStep;
import com.cleardocs.repository.jpa.DocumentRepository;
import com.cleardocs.repository.jpa.WorkflowRepository;
import com.cleardocs.security.UserPrincipal;
import com.cleardocs.statemachine.DocumentEvent;
import com.cleardocs.statemachine.DocumentState;
import com.cleardocs.statemachine.DocumentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalService {

    private final DocumentRepository documentRepository;
    private final WorkflowRepository workflowRepository;
    private final DocumentStateMachine stateMachine;
    private final DocumentService documentService;

    @Audited(action = "DOCUMENT_REVIEW_ACTION", resourceType = "APPROVAL")
    @Transactional
    @CacheEvict(value = "documents", key = "#documentId")
    public DocumentResponse processReviewAction(Long documentId, ApprovalRequest request, UserPrincipal principal) {
        Document document = documentRepository.findByIdWithLock(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        ApprovalWorkflow workflow = workflowRepository
            .findActiveWorkflowWithSteps(documentId)
            .orElseThrow(() -> new IllegalStateException("No active workflow for document " + documentId));

        WorkflowStep currentStep = getCurrentPendingStep(workflow, principal.getId());
        DocumentEvent event = parseEvent(request.getAction());
        DocumentState newState = stateMachine.transition(document.getState(), event);

        currentStep.setStatus(mapEventToStepStatus(event));
        currentStep.setComments(request.getComments());
        currentStep.setActionTaken(request.getAction());
        currentStep.setActedAt(Instant.now());

        document.setState(newState);
        applyStateSpecificFields(document, request, newState);

        if (newState == DocumentState.APPROVED || newState == DocumentState.REJECTED) {
            workflow.setStatus("COMPLETED");
            workflow.setCompletedAt(Instant.now());
        } else if (newState == DocumentState.PENDING_APPROVAL) {
            advanceWorkflowStep(workflow);
        }

        documentRepository.save(document);
        workflowRepository.save(workflow);
        log.info("Review action {} applied to document {} -> {}", request.getAction(), documentId, newState);
        return documentService.toResponse(document);
    }

    @Audited(action = "DOCUMENT_START_REVIEW", resourceType = "APPROVAL")
    @Transactional
    @CacheEvict(value = "documents", key = "#documentId")
    public DocumentResponse startReview(Long documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdWithLock(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        DocumentState newState = stateMachine.transition(document.getState(), DocumentEvent.START_REVIEW);
        document.setState(newState);
        documentRepository.save(document);
        return documentService.toResponse(document);
    }

    @Transactional(readOnly = true)
    public List<WorkflowStepResponse> getWorkflowSteps(Long documentId) {
        return workflowRepository.findActiveWorkflowWithSteps(documentId)
            .map(wf -> wf.getSteps().stream().map(this::toStepResponse).collect(Collectors.toList()))
            .orElse(java.util.Collections.emptyList());
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getPendingForReviewer(UserPrincipal principal) {
        return workflowRepository.findPendingWorkflowsForReviewer(principal.getId())
            .stream()
            .map(wf -> documentService.toResponse(wf.getDocument()))
            .collect(Collectors.toList());
    }

    private WorkflowStep getCurrentPendingStep(ApprovalWorkflow workflow, Long reviewerId) {
        return workflow.getSteps().stream()
            .filter(s -> "PENDING".equals(s.getStatus()))
            .filter(s -> s.getReviewer() != null && s.getReviewer().getId().equals(reviewerId))
            .findFirst()
            .orElseThrow(() -> new AccessDeniedException("No pending step assigned to you for this document"));
    }

    private void advanceWorkflowStep(ApprovalWorkflow workflow) {
        int nextOrder = workflow.getCurrentStep() + 1;
        workflow.setCurrentStep(nextOrder);
        workflow.getSteps().stream()
            .filter(s -> s.getStepOrder().equals(nextOrder))
            .findFirst()
            .ifPresent(s -> s.setStatus("PENDING"));
    }

    private DocumentEvent parseEvent(String action) {
        switch (action.toUpperCase()) {
            case "APPROVE":           return DocumentEvent.COMPLETE_REVIEW;
            case "REJECT":            return DocumentEvent.REJECT;
            case "REQUEST_REVISION":  return DocumentEvent.REQUEST_REVISION;
            default: throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private String mapEventToStepStatus(DocumentEvent event) {
        switch (event) {
            case COMPLETE_REVIEW:    return "APPROVED";
            case REJECT:             return "REJECTED";
            case REQUEST_REVISION:   return "REVISION_REQUESTED";
            default:                 return "COMPLETED";
        }
    }

    private void applyStateSpecificFields(Document document, ApprovalRequest request, DocumentState newState) {
        if (newState == DocumentState.APPROVED) {
            document.setApprovedAt(Instant.now());
        } else if (newState == DocumentState.REJECTED) {
            document.setRejectedAt(Instant.now());
            document.setRejectionReason(request.getRejectionReason());
        }
    }

    private WorkflowStepResponse toStepResponse(WorkflowStep step) {
        return WorkflowStepResponse.builder()
            .id(step.getId())
            .stepOrder(step.getStepOrder())
            .reviewerId(step.getReviewer() != null ? step.getReviewer().getId() : null)
            .reviewerName(step.getReviewer() != null ? step.getReviewer().getFullName() : null)
            .reviewerRole(step.getReviewerRole())
            .status(step.getStatus())
            .comments(step.getComments())
            .actionTaken(step.getActionTaken())
            .dueDate(step.getDueDate())
            .actedAt(step.getActedAt())
            .build();
    }
}
