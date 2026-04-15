package com.grainflow.warehouse.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByCompanyId(UUID companyId, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndEntityType(UUID companyId, String entityType, Pageable pageable);

    Page<AuditLog> findByCompanyIdAndEntityTypeAndEntityId(UUID companyId, String entityType, UUID entityId, Pageable pageable);
}
