package com.eventbudget.service;

import com.eventbudget.model.approval.AuditLog;
import com.eventbudget.model.user.User;
import com.eventbudget.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String entityType, Long entityId, String action, User performedBy, String description) {
        AuditLog log = new AuditLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setPerformedBy(performedBy);
        log.setDescription(description);
        auditLogRepository.save(log);
    }
}
