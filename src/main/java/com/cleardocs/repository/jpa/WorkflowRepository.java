package com.cleardocs.repository.jpa;

import com.cleardocs.model.jpa.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {

    Optional<ApprovalWorkflow> findByDocumentIdAndStatus(Long documentId, String status);

    List<ApprovalWorkflow> findByDocumentId(Long documentId);

    @Query("""
        SELECT w FROM ApprovalWorkflow w
        JOIN FETCH w.steps s
        JOIN FETCH s.reviewer r
        WHERE w.document.id = :documentId AND w.status = 'ACTIVE'
        """)
    Optional<ApprovalWorkflow> findActiveWorkflowWithSteps(@Param("documentId") Long documentId);

    @Query("""
        SELECT w FROM ApprovalWorkflow w
        JOIN w.steps s
        WHERE s.reviewer.id = :reviewerId AND s.status = 'PENDING' AND w.status = 'ACTIVE'
        """)
    List<ApprovalWorkflow> findPendingWorkflowsForReviewer(@Param("reviewerId") Long reviewerId);
}
