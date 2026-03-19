package com.eventbudget.model.approval;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workflow_configs")
@Getter
@Setter
public class WorkflowConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long configId;

    @Column(nullable = false)
    private String name;

    @Column(precision = 15, scale = 2)
    private BigDecimal singleLevelLimit;

    @Column(precision = 15, scale = 2)
    private BigDecimal multiLevelThreshold;

    @Column(precision = 15, scale = 2)
    private BigDecimal autoApproveLimit;

    public ApprovalLevel resolveLevel(BigDecimal amount) {
        if (autoApproveLimit != null && amount.compareTo(autoApproveLimit) <= 0)
            return ApprovalLevel.AUTO_APPROVED;
        if (multiLevelThreshold != null && amount.compareTo(multiLevelThreshold) > 0)
            return ApprovalLevel.MULTI_LEVEL;
        return ApprovalLevel.SINGLE_LEVEL;
    }
}