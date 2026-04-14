package com.grainflow.warehouse.fixture;

import com.grainflow.warehouse.dto.batch.BatchResponse;
import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class BatchTestFixtures {

    // --- Company A ---
    public static final UUID COMPANY_A_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    public static final AuthenticatedUser managerA = new AuthenticatedUser(
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            COMPANY_A_ID,
            "manager.a@grainflow.com",
            "MANAGER"
    );
    public static final AuthenticatedUser workerA1 = new AuthenticatedUser(
            UUID.fromString("22222222-2222-2222-2222-222222222222"),
            COMPANY_A_ID,
            "worker.a1@grainflow.com",
            "WORKER"
    );
    public static final AuthenticatedUser workerA2 = new AuthenticatedUser(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            COMPANY_A_ID,
            "worker.a2@grainflow.com",
            "WORKER"
    );

    // --- Company B ---
    public static final UUID COMPANY_B_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    public static final AuthenticatedUser managerB = new AuthenticatedUser(
            UUID.fromString("44444444-4444-4444-4444-444444444444"),
            COMPANY_B_ID,
            "manager.b@grainflow.com",
            "MANAGER"
    );
    public static final AuthenticatedUser workerB1 = new AuthenticatedUser(
            UUID.fromString("55555555-5555-5555-5555-555555555555"),
            COMPANY_B_ID,
            "worker.b1@grainflow.com",
            "WORKER"
    );

    // --- Sample batch belonging to Company A ---
    public static final UUID BATCH_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    public static BatchResponse sampleBatch() {
        return new BatchResponse(
                BATCH_ID,
                COMPANY_A_ID,
                "CONTRACT-2026-001",
                CultureType.WHEAT,
                BatchStatus.PLANNED,
                new BigDecimal("500.000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "test batch",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }

    public static BatchResponse activeBatch() {
        return new BatchResponse(
                BATCH_ID,
                COMPANY_A_ID,
                "CONTRACT-2026-001",
                CultureType.WHEAT,
                BatchStatus.ACTIVE,
                new BigDecimal("500.000"),
                new BigDecimal("100.000"),
                BigDecimal.ZERO,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "test batch",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0)
        );
    }
}
