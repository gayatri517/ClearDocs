package com.cleardocs.model.jpa;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "workflow_steps", indexes = {
    @Index(name = "idx_step_workflow", columnList = "workflow_id"),
    @Index(name = "idx_step_reviewer", columnList = "reviewer_id"),
    @Index(name = "idx_step_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "step_seq")
    @SequenceGenerator(name = "step_seq", sequenceName = "step_sequence", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "reviewer_role", length = 50)
    private String reviewerRole;

    @Column(name = "status", length = 30)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    @Column(name = "action_taken", length = 30)
    private String actionTaken;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "acted_at")
    private Instant actedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;
}
