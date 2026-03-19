package com.eventbudget.model.approval;

import java.time.LocalDateTime;

import com.eventbudget.model.user.ApprovingAuthority;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "approval_steps")
@Getter
@Setter
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private ApprovalWorkflow workflow;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", nullable = false)
    private ApprovingAuthority assignedTo;

    private int stepOrder;
    private String decision;
    private String comment;
    private LocalDateTime decidedAt;

    public boolean isDecided() {
        return decision != null && !decision.equals("PENDING");
    }

    public void resolve(String decision, String comment) {
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = LocalDateTime.now();
    }
}