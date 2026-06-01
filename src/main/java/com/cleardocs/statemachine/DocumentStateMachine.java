package com.cleardocs.statemachine;

import com.cleardocs.exception.InvalidStateTransitionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class DocumentStateMachine {

    private static final Map<DocumentState, Map<DocumentEvent, DocumentState>> TRANSITIONS =
        new EnumMap<>(DocumentState.class);

    static {
        Map<DocumentEvent, DocumentState> draftT = new EnumMap<>(DocumentEvent.class);
        draftT.put(DocumentEvent.SUBMIT, DocumentState.SUBMITTED);
        draftT.put(DocumentEvent.WITHDRAW, DocumentState.WITHDRAWN);
        TRANSITIONS.put(DocumentState.DRAFT, draftT);

        Map<DocumentEvent, DocumentState> submittedT = new EnumMap<>(DocumentEvent.class);
        submittedT.put(DocumentEvent.START_REVIEW, DocumentState.UNDER_REVIEW);
        submittedT.put(DocumentEvent.WITHDRAW, DocumentState.WITHDRAWN);
        TRANSITIONS.put(DocumentState.SUBMITTED, submittedT);

        Map<DocumentEvent, DocumentState> underReviewT = new EnumMap<>(DocumentEvent.class);
        underReviewT.put(DocumentEvent.COMPLETE_REVIEW, DocumentState.PENDING_APPROVAL);
        underReviewT.put(DocumentEvent.REQUEST_REVISION, DocumentState.REVISION_REQUESTED);
        underReviewT.put(DocumentEvent.REJECT, DocumentState.REJECTED);
        TRANSITIONS.put(DocumentState.UNDER_REVIEW, underReviewT);

        Map<DocumentEvent, DocumentState> pendingT = new EnumMap<>(DocumentEvent.class);
        pendingT.put(DocumentEvent.APPROVE, DocumentState.APPROVED);
        pendingT.put(DocumentEvent.REJECT, DocumentState.REJECTED);
        pendingT.put(DocumentEvent.REQUEST_REVISION, DocumentState.REVISION_REQUESTED);
        TRANSITIONS.put(DocumentState.PENDING_APPROVAL, pendingT);

        Map<DocumentEvent, DocumentState> revisionT = new EnumMap<>(DocumentEvent.class);
        revisionT.put(DocumentEvent.SUBMIT, DocumentState.SUBMITTED);
        revisionT.put(DocumentEvent.WITHDRAW, DocumentState.WITHDRAWN);
        TRANSITIONS.put(DocumentState.REVISION_REQUESTED, revisionT);

        Map<DocumentEvent, DocumentState> rejectedT = new EnumMap<>(DocumentEvent.class);
        rejectedT.put(DocumentEvent.SUBMIT, DocumentState.DRAFT);
        rejectedT.put(DocumentEvent.WITHDRAW, DocumentState.WITHDRAWN);
        TRANSITIONS.put(DocumentState.REJECTED, rejectedT);

        Map<DocumentEvent, DocumentState> approvedT = new EnumMap<>(DocumentEvent.class);
        approvedT.put(DocumentEvent.ARCHIVE, DocumentState.ARCHIVED);
        TRANSITIONS.put(DocumentState.APPROVED, approvedT);

        TRANSITIONS.put(DocumentState.WITHDRAWN, new EnumMap<>(DocumentEvent.class));
        TRANSITIONS.put(DocumentState.ARCHIVED, new EnumMap<>(DocumentEvent.class));
    }

    public DocumentState transition(DocumentState currentState, DocumentEvent event) {
        Map<DocumentEvent, DocumentState> allowed = TRANSITIONS.get(currentState);
        if (allowed == null || !allowed.containsKey(event)) {
            throw new InvalidStateTransitionException(
                String.format("Cannot apply event '%s' to document in state '%s'. Allowed events: %s",
                    event, currentState, getAllowedEvents(currentState))
            );
        }
        DocumentState next = allowed.get(event);
        log.info("Document state transition: {} --[{}]--> {}", currentState, event, next);
        return next;
    }

    public Set<DocumentEvent> getAllowedEvents(DocumentState state) {
        Map<DocumentEvent, DocumentState> t = TRANSITIONS.get(state);
        if (t == null || t.isEmpty()) return Collections.emptySet();
        return EnumSet.copyOf(t.keySet());
    }

    public boolean canTransition(DocumentState currentState, DocumentEvent event) {
        Map<DocumentEvent, DocumentState> t = TRANSITIONS.get(currentState);
        return t != null && t.containsKey(event);
    }
}
