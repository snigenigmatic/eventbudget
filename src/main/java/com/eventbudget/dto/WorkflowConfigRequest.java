package com.eventbudget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record WorkflowConfigRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.00") BigDecimal singleLevelLimit,
        @NotNull @DecimalMin(value = "0.00") BigDecimal multiLevelThreshold,
        @NotNull @DecimalMin(value = "0.00") BigDecimal autoApproveLimit
) {
}
