package com.grainflow.warehouse.audit;

import com.grainflow.warehouse.dto.ApiResponse;
import com.grainflow.warehouse.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ApiResponse<Page<AuditLogResponse>> getAll(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        Page<AuditLog> page;

        if (entityType != null && entityId != null) {
            page = auditLogRepository.findByCompanyIdAndEntityTypeAndEntityId(
                    user.companyId(), entityType, entityId, pageable);
        } else if (entityType != null) {
            page = auditLogRepository.findByCompanyIdAndEntityType(
                    user.companyId(), entityType, pageable);
        } else {
            page = auditLogRepository.findByCompanyId(user.companyId(), pageable);
        }

        return ApiResponse.success(page.map(AuditLogResponse::from));
    }
}
