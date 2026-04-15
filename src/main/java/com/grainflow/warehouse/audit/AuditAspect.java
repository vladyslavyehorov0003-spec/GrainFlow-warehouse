package com.grainflow.warehouse.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grainflow.warehouse.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Runs after the method returns successfully (no audit on exception)
    @AfterReturning(
            pointcut = "@annotation(auditable)",
            returning = "result"
    )
    public void audit(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            AuthenticatedUser user = currentUser();
            if (user == null) return;

            UUID entityId   = extractEntityId(result);
            String changes  = serializeArgs(joinPoint.getArgs());

            AuditLog log = AuditLog.builder()
                    .userId(user.userId())
                    .companyId(user.companyId())
                    .action(auditable.action())
                    .entityType(auditable.entityType())
                    .entityId(entityId)
                    .changes(changes)
                    .build();

            auditLogRepository.save(log);

        } catch (Exception e) {
            // Audit must never break business logic
            log.warn("Audit failed for {}: {}", auditable.action(), e.getMessage());
        }
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private AuthenticatedUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return u;
        }
        return null;
    }

    // Try to read the returned DTO's "id" field via Jackson
    private UUID extractEntityId(Object result) {
        try {
            if (result == null) return null;
            Map<?, ?> map = objectMapper.convertValue(result, Map.class);
            Object id = map.get("id");
            return id != null ? UUID.fromString(id.toString()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // Serialize non-null, non-UUID method arguments (skip companyId / pageable)
    private String serializeArgs(Object[] args) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            for (Object arg : args) {
                if (arg == null)            continue;
                if (arg instanceof UUID)    continue; // skip id / companyId params
                data.put(arg.getClass().getSimpleName(), arg);
            }
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }
}
