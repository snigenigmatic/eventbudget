package com.eventbudget.dto;

import com.eventbudget.model.approval.WorkflowConfig;
import java.math.BigDecimal;

public record WorkflowConfigResponse(
        Long configId,
        String name,
        BigDecimal singleLevelLimit,
        BigDecimal multiLevelThreshold,
        BigDecimal autoApproveLimit
) {

    public static WorkflowConfigResponse from(WorkflowConfig config) {
        return new WorkflowConfigResponse(
                config.getConfigId(),
                config.getName(),
                config.getSingleLevelLimit(),
                config.getMultiLevelThreshold(),
                config.getAutoApproveLimit());
    }
}
