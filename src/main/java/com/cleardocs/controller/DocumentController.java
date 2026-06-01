package com.cleardocs.controller;

import com.cleardocs.dto.DocumentRequest;
import com.cleardocs.dto.DocumentResponse;
import com.cleardocs.security.UserPrincipal;
import com.cleardocs.service.DocumentService;
import com.cleardocs.statemachine.DocumentState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a new document with optional file attachment")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestPart("data") DocumentRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(documentService.createDocument(request, file, principal));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a document by ID")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @GetMapping
    @Operation(summary = "List and filter documents")
    public ResponseEntity<Page<DocumentResponse>> listDocuments(
            @RequestParam(required = false) DocumentState state,
            @RequestParam(required = false) Long submitterId,
            @RequestParam(required = false) String documentType,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(documentService.listDocuments(state, submitterId, documentType, pageable));
    }

    @GetMapping("/mine")
    @Operation(summary = "Get the current user's documents")
    public ResponseEntity<Page<DocumentResponse>> getMyDocuments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(documentService.getMyDocuments(principal, pageable));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT or REVISION_REQUESTED document")
    public ResponseEntity<DocumentResponse> updateDocument(
            @PathVariable Long id,
            @Valid @RequestBody DocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.updateDocument(id, request, principal));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a document for review")
    public ResponseEntity<DocumentResponse> submitDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.submitDocument(id, principal));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw a document")
    public ResponseEntity<DocumentResponse> withdrawDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.withdrawDocument(id, principal));
    }

    @GetMapping("/stats/state-counts")
    @PreAuthorize("hasAnyRole('ADMIN','AUDITOR','APPROVER')")
    @Operation(summary = "Get document counts grouped by state")
    public ResponseEntity<Map<String, Long>> getStateCounts() {
        return ResponseEntity.ok(documentService.getStateCounts());
    }
}
