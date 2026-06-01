package com.cleardocs.service;

import com.cleardocs.aop.AuditAspect.Audited;
import com.cleardocs.dto.DocumentRequest;
import com.cleardocs.dto.DocumentResponse;
import com.cleardocs.exception.DocumentNotFoundException;
import com.cleardocs.model.jpa.ApprovalWorkflow;
import com.cleardocs.model.jpa.Document;
import com.cleardocs.model.jpa.User;
import com.cleardocs.model.jpa.WorkflowStep;
import com.cleardocs.model.mongo.DocumentMetadata;
import com.cleardocs.repository.jpa.DocumentRepository;
import com.cleardocs.repository.jpa.UserRepository;
import com.cleardocs.repository.jpa.WorkflowRepository;
import com.cleardocs.repository.mongo.DocumentMetadataRepository;
import com.cleardocs.security.UserPrincipal;
import com.cleardocs.statemachine.DocumentEvent;
import com.cleardocs.statemachine.DocumentState;
import com.cleardocs.statemachine.DocumentStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final WorkflowRepository workflowRepository;
    private final UserRepository userRepository;
    private final DocumentMetadataRepository metadataRepository;
    private final DocumentStateMachine stateMachine;
    private final FileStorageService fileStorageService;

    @Audited(action = "DOCUMENT_CREATE", resourceType = "DOCUMENT", description = "Create new document")
    @Transactional
    public DocumentResponse createDocument(DocumentRequest request, MultipartFile file, UserPrincipal principal) {
        User submitter = userRepository.findById(principal.getId())
            .orElseThrow(() -> new IllegalArgumentException("Submitter not found"));

        String refNumber = generateReferenceNumber(request.getDocumentType());

        Document document = Document.builder()
            .referenceNumber(refNumber)
            .title(request.getTitle())
            .description(request.getDescription())
            .documentType(request.getDocumentType())
            .priority(request.getPriority())
            .submitter(submitter)
            .state(DocumentState.DRAFT)
            .build();

        if (file != null && !file.isEmpty()) {
            String filePath = fileStorageService.store(file, refNumber);
            document.setFilePath(filePath);
            document.setFileName(file.getOriginalFilename());
            document.setFileSize(file.getSize());
            document.setContentType(file.getContentType());
        }

        Document saved = documentRepository.save(document);

        if (request.getReviewerIds() != null && !request.getReviewerIds().isEmpty()) {
            createWorkflow(saved, request.getReviewerIds());
        }

        DocumentMetadata metadata = DocumentMetadata.builder()
            .documentId(saved.getId())
            .referenceNumber(refNumber)
            .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
            .customFields(request.getCustomFields() != null ? request.getCustomFields() : new java.util.HashMap<>())
            .searchKeywords(buildKeywords(saved))
            .build();
        metadataRepository.save(metadata);

        return toResponse(saved);
    }

    @Audited(action = "DOCUMENT_SUBMIT", resourceType = "DOCUMENT", description = "Submit document for review")
    @Transactional
    @CacheEvict(value = "documents", key = "#documentId")
    public DocumentResponse submitDocument(Long documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdWithLock(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getSubmitter().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the document owner can submit it");
        }

        DocumentState newState = stateMachine.transition(document.getState(), DocumentEvent.SUBMIT);
        document.setState(newState);
        document.setSubmittedAt(Instant.now());

        return toResponse(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "documents", key = "#id")
    public DocumentResponse getDocument(Long id) {
        Document document = documentRepository.findById(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(DocumentState state, Long submitterId,
                                                 String documentType, Pageable pageable) {
        return documentRepository.search(state, submitterId, documentType, null, null, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> getMyDocuments(UserPrincipal principal, Pageable pageable) {
        return documentRepository.findBySubmitterId(principal.getId(), pageable).map(this::toResponse);
    }

    @Audited(action = "DOCUMENT_UPDATE", resourceType = "DOCUMENT", description = "Update document")
    @Transactional
    @CacheEvict(value = "documents", key = "#id")
    public DocumentResponse updateDocument(Long id, DocumentRequest request, UserPrincipal principal) {
        Document document = documentRepository.findByIdWithLock(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));

        if (!document.getSubmitter().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the document owner can update it");
        }
        if (document.getState() != DocumentState.DRAFT && document.getState() != DocumentState.REVISION_REQUESTED) {
            throw new IllegalStateException("Document can only be updated in DRAFT or REVISION_REQUESTED state");
        }

        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setPriority(request.getPriority());

        return toResponse(documentRepository.save(document));
    }

    @Audited(action = "DOCUMENT_WITHDRAW", resourceType = "DOCUMENT", description = "Withdraw document")
    @Transactional
    @CacheEvict(value = "documents", key = "#documentId")
    public DocumentResponse withdrawDocument(Long documentId, UserPrincipal principal) {
        Document document = documentRepository.findByIdWithLock(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));

        if (!document.getSubmitter().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the document owner can withdraw it");
        }

        DocumentState newState = stateMachine.transition(document.getState(), DocumentEvent.WITHDRAW);
        document.setState(newState);
        return toResponse(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Long> getStateCounts() {
        List<Object[]> rows = documentRepository.countByStateGrouped();
        java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (DocumentState s : DocumentState.values()) counts.put(s.name(), 0L);
        for (Object[] row : rows) counts.put(((DocumentState) row[0]).name(), (Long) row[1]);
        return counts;
    }

    private void createWorkflow(Document document, List<Long> reviewerIds) {
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
            .document(document)
            .status("ACTIVE")
            .currentStep(0)
            .build();

        List<WorkflowStep> steps = new ArrayList<>();
        for (int i = 0; i < reviewerIds.size(); i++) {
            User reviewer = userRepository.findById(reviewerIds.get(i))
                .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));
            WorkflowStep step = WorkflowStep.builder()
                .workflow(workflow)
                .stepOrder(i + 1)
                .reviewer(reviewer)
                .status("PENDING")
                .dueDate(Instant.now().plusSeconds(7 * 24 * 3600))
                .build();
            steps.add(step);
        }
        workflow.setSteps(steps);
        workflowRepository.save(workflow);
    }

    private String generateReferenceNumber(String documentType) {
        String prefix = (documentType != null && documentType.length() >= 3)
            ? documentType.substring(0, 3).toUpperCase()
            : "DOC";
        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneOffset.UTC).format(Instant.now());
        return prefix + "-" + timestamp;
    }

    private List<String> buildKeywords(Document doc) {
        List<String> kw = new ArrayList<>();
        if (doc.getTitle() != null) kw.addAll(List.of(doc.getTitle().split("\\s+")));
        if (doc.getDocumentType() != null) kw.add(doc.getDocumentType());
        if (doc.getReferenceNumber() != null) kw.add(doc.getReferenceNumber());
        return kw.stream().map(String::toLowerCase).distinct().collect(Collectors.toList());
    }

    public DocumentResponse toResponse(Document doc) {
        return DocumentResponse.builder()
            .id(doc.getId())
            .referenceNumber(doc.getReferenceNumber())
            .title(doc.getTitle())
            .description(doc.getDescription())
            .documentType(doc.getDocumentType())
            .priority(doc.getPriority())
            .state(doc.getState())
            .fileName(doc.getFileName())
            .fileSize(doc.getFileSize())
            .contentType(doc.getContentType())
            .submitterId(doc.getSubmitter() != null ? doc.getSubmitter().getId() : null)
            .submitterName(doc.getSubmitter() != null ? doc.getSubmitter().getFullName() : null)
            .submitterEmail(doc.getSubmitter() != null ? doc.getSubmitter().getEmail() : null)
            .submittedAt(doc.getSubmittedAt())
            .approvedAt(doc.getApprovedAt())
            .rejectedAt(doc.getRejectedAt())
            .rejectionReason(doc.getRejectionReason())
            .createdAt(doc.getCreatedAt())
            .updatedAt(doc.getUpdatedAt())
            .version(doc.getVersion())
            .build();
    }
}
