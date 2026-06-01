package com.cleardocs.statemachine;

public enum DocumentState {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    REVISION_REQUESTED,
    WITHDRAWN,
    ARCHIVED;

    public boolean canTransitionTo(DocumentState target) {
        switch (this) {
            case DRAFT:              return target == SUBMITTED || target == WITHDRAWN;
            case SUBMITTED:          return target == UNDER_REVIEW || target == WITHDRAWN;
            case UNDER_REVIEW:       return target == PENDING_APPROVAL || target == REVISION_REQUESTED || target == REJECTED;
            case PENDING_APPROVAL:   return target == APPROVED || target == REJECTED || target == REVISION_REQUESTED;
            case REVISION_REQUESTED: return target == SUBMITTED || target == WITHDRAWN;
            case APPROVED:           return target == ARCHIVED;
            case REJECTED:           return target == DRAFT || target == WITHDRAWN;
            default:                 return false;
        }
    }
}
