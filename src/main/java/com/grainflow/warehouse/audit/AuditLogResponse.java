package com.grainflow.warehouse.audit;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID userId,
        UUID companyId,
        String action,
        String entityType,
        UUID entityId,
        String changes,
        LocalDateTime createdAt
) {
    static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getUserId(),
                log.getCompanyId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getChanges(),
                log.getCreatedAt()
        );
    }
}
