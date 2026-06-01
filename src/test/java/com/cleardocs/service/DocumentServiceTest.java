package com.cleardocs.service;

import com.cleardocs.dto.DocumentRequest;
import com.cleardocs.dto.DocumentResponse;
import com.cleardocs.exception.DocumentNotFoundException;
import com.cleardocs.model.jpa.Document;
import com.cleardocs.model.jpa.User;
import com.cleardocs.repository.jpa.DocumentRepository;
import com.cleardocs.repository.jpa.UserRepository;
import com.cleardocs.repository.jpa.WorkflowRepository;
import com.cleardocs.repository.mongo.DocumentMetadataRepository;
import com.cleardocs.security.UserPrincipal;
import com.cleardocs.statemachine.DocumentState;
import com.cleardocs.statemachine.DocumentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentService Unit Tests")
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private WorkflowRepository workflowRepository;
    @Mock private UserRepository userRepository;
    @Mock private DocumentMetadataRepository metadataRepository;
    @Mock private DocumentStateMachine stateMachine;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private DocumentService documentService;

    private User testUser;
    private UserPrincipal testPrincipal;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .fullName("Test User")
            .enabled(true)
            .accountNonLocked(true)
            .build();

        testPrincipal = UserPrincipal.create(testUser);

        testDocument = Document.builder()
            .id(1L)
            .referenceNumber("DOC-20240101120000")
            .title("Test Document")
            .description("Test Description")
            .documentType("CONTRACT")
            .priority("NORMAL")
            .submitter(testUser)
            .state(DocumentState.DRAFT)
            .version(0L)
            .build();
    }

    @Test
    @DisplayName("getDocument - returns response when document exists")
    void getDocument_existingId_returnsResponse() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(testDocument));

        DocumentResponse response = documentService.getDocument(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Test Document");
        assertThat(response.getState()).isEqualTo(DocumentState.DRAFT);
    }

    @Test
    @DisplayName("getDocument - throws DocumentNotFoundException when not found")
    void getDocument_nonExistingId_throwsException() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(99L))
            .isInstanceOf(DocumentNotFoundException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("submitDocument - transitions state to SUBMITTED")
    void submitDocument_validDraft_transitionsToSubmitted() {
        when(documentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testDocument));
        when(stateMachine.transition(DocumentState.DRAFT, com.cleardocs.statemachine.DocumentEvent.SUBMIT))
            .thenReturn(DocumentState.SUBMITTED);
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);

        DocumentResponse response = documentService.submitDocument(1L, testPrincipal);

        verify(documentRepository).save(argThat(d -> d.getState() == DocumentState.SUBMITTED));
    }

    @Test
    @DisplayName("submitDocument - throws AccessDeniedException for non-owner")
    void submitDocument_nonOwner_throwsAccessDeniedException() {
        UserPrincipal otherUser = new UserPrincipal(
            99L, "other", "other@example.com", "Other",
            "pass", true, true, Collections.emptyList()
        );
        when(documentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testDocument));

        assertThatThrownBy(() -> documentService.submitDocument(1L, otherUser))
            .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("getStateCounts - returns map for all states")
    void getStateCounts_returnsAllStateKeys() {
        when(documentRepository.countByStateGrouped()).thenReturn(Collections.emptyList());

        java.util.Map<String, Long> counts = documentService.getStateCounts();

        assertThat(counts).containsKeys(DocumentState.DRAFT.name(), DocumentState.APPROVED.name());
        assertThat(counts.values()).allMatch(v -> v >= 0);
    }
}
