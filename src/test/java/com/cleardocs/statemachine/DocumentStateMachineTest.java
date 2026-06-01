package com.cleardocs.statemachine;

import com.cleardocs.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DocumentStateMachine Unit Tests")
class DocumentStateMachineTest {

    private DocumentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new DocumentStateMachine();
    }

    @Test
    @DisplayName("DRAFT + SUBMIT -> SUBMITTED")
    void transition_draftSubmit_returnsSubmitted() {
        assertThat(stateMachine.transition(DocumentState.DRAFT, DocumentEvent.SUBMIT))
            .isEqualTo(DocumentState.SUBMITTED);
    }

    @Test
    @DisplayName("SUBMITTED + START_REVIEW -> UNDER_REVIEW")
    void transition_submittedStartReview_returnsUnderReview() {
        assertThat(stateMachine.transition(DocumentState.SUBMITTED, DocumentEvent.START_REVIEW))
            .isEqualTo(DocumentState.UNDER_REVIEW);
    }

    @Test
    @DisplayName("UNDER_REVIEW + COMPLETE_REVIEW -> PENDING_APPROVAL")
    void transition_underReviewComplete_returnsPendingApproval() {
        assertThat(stateMachine.transition(DocumentState.UNDER_REVIEW, DocumentEvent.COMPLETE_REVIEW))
            .isEqualTo(DocumentState.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("PENDING_APPROVAL + APPROVE -> APPROVED")
    void transition_pendingApprovalApprove_returnsApproved() {
        assertThat(stateMachine.transition(DocumentState.PENDING_APPROVAL, DocumentEvent.APPROVE))
            .isEqualTo(DocumentState.APPROVED);
    }

    @Test
    @DisplayName("PENDING_APPROVAL + REJECT -> REJECTED")
    void transition_pendingApprovalReject_returnsRejected() {
        assertThat(stateMachine.transition(DocumentState.PENDING_APPROVAL, DocumentEvent.REJECT))
            .isEqualTo(DocumentState.REJECTED);
    }

    @Test
    @DisplayName("UNDER_REVIEW + REQUEST_REVISION -> REVISION_REQUESTED")
    void transition_underReviewRequestRevision_returnsRevisionRequested() {
        assertThat(stateMachine.transition(DocumentState.UNDER_REVIEW, DocumentEvent.REQUEST_REVISION))
            .isEqualTo(DocumentState.REVISION_REQUESTED);
    }

    @Test
    @DisplayName("APPROVED + ARCHIVE -> ARCHIVED")
    void transition_approvedArchive_returnsArchived() {
        assertThat(stateMachine.transition(DocumentState.APPROVED, DocumentEvent.ARCHIVE))
            .isEqualTo(DocumentState.ARCHIVED);
    }

    @Test
    @DisplayName("ARCHIVED is a terminal state — no transitions allowed")
    void transition_archivedAnyEvent_throwsException() {
        assertThatThrownBy(() -> stateMachine.transition(DocumentState.ARCHIVED, DocumentEvent.SUBMIT))
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("WITHDRAWN is a terminal state — no transitions allowed")
    void transition_withdrawnAnyEvent_throwsException() {
        assertThatThrownBy(() -> stateMachine.transition(DocumentState.WITHDRAWN, DocumentEvent.SUBMIT))
            .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("Invalid transition from DRAFT + APPROVE throws exception")
    void transition_invalidEvent_throwsInvalidStateTransitionException() {
        assertThatThrownBy(() -> stateMachine.transition(DocumentState.DRAFT, DocumentEvent.APPROVE))
            .isInstanceOf(InvalidStateTransitionException.class)
            .hasMessageContaining("DRAFT")
            .hasMessageContaining("APPROVE");
    }

    @ParameterizedTest(name = "{0} can transition to WITHDRAWN: {1}")
    @CsvSource({
        "DRAFT, true",
        "SUBMITTED, true",
        "REVISION_REQUESTED, true",
        "APPROVED, false",
        "ARCHIVED, false"
    })
    void canTransition_withdraw(DocumentState state, boolean expected) {
        assertThat(stateMachine.canTransition(state, DocumentEvent.WITHDRAW)).isEqualTo(expected);
    }

    @Test
    @DisplayName("getAllowedEvents returns non-empty set for active states")
    void getAllowedEvents_activeState_returnsNonEmpty() {
        assertThat(stateMachine.getAllowedEvents(DocumentState.DRAFT)).isNotEmpty();
        assertThat(stateMachine.getAllowedEvents(DocumentState.SUBMITTED)).isNotEmpty();
    }

    @Test
    @DisplayName("getAllowedEvents returns empty set for terminal states")
    void getAllowedEvents_terminalState_returnsEmpty() {
        assertThat(stateMachine.getAllowedEvents(DocumentState.ARCHIVED)).isEmpty();
        assertThat(stateMachine.getAllowedEvents(DocumentState.WITHDRAWN)).isEmpty();
    }
}
