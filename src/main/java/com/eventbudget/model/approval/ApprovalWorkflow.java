package com.eventbudget.model.approval;

import com.eventbudget.model.domain.ExpenseClaim;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "approval_workflows")
@Getter
@Setter
public class ApprovalWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workflowId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id", nullable = false)
    private ExpenseClaim claim;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    private int currentStepIndex = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private WorkflowConfig config;

    public ApprovalStep getCurrentStep() {
        if (steps.isEmpty() || currentStepIndex >= steps.size()) return null;
        return steps.get(currentStepIndex);
    }

    public void advance() {
        this.currentStepIndex++;
    }

    public boolean isComplete() {
        return currentStepIndex >= steps.size();
    }

    public boolean isMultiLevel() {
        return steps.size() > 1;
    }
}