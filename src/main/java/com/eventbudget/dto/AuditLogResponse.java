package com.eventbudget.dto;

import com.eventbudget.model.approval.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long logId,
        String entityType,
        Long entityId,
        String action,
        String description,
        UserSummaryResponse performedBy,
        LocalDateTime timestamp
) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getLogId(),
                log.getEntityType(),
                log.getEntityId(),
                log.getAction(),
                log.getDescription(),
                log.getPerformedBy() != null ? UserSummaryResponse.from(log.getPerformedBy()) : null,
                log.getTimestamp());
    }
}
