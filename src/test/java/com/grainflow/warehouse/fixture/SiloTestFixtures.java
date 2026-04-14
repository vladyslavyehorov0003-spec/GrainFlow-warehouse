package com.grainflow.warehouse.fixture;

import com.grainflow.warehouse.dto.silo.SiloResponse;
import com.grainflow.warehouse.entity.CultureType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.grainflow.warehouse.fixture.BatchTestFixtures.COMPANY_A_ID;
import static com.grainflow.warehouse.fixture.BatchTestFixtures.COMPANY_B_ID;

public class SiloTestFixtures {

    public static final UUID SILO_ID   = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    public static final UUID SILO_B_ID = UUID.fromString("bbbb1111-bbbb-bbbb-bbbb-bbbb11111111");

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 4, 14, 8, 0);

    public static SiloResponse emptySilo() {
        return new SiloResponse(
                SILO_ID, COMPANY_A_ID,
                "Silo-A1",
                new BigDecimal("500.000"),
                BigDecimal.ZERO,
                null,
                null,
                CREATED, CREATED
        );
    }

    public static SiloResponse siloWithGrain() {
        return new SiloResponse(
                SILO_ID, COMPANY_A_ID,
                "Silo-A1",
                new BigDecimal("500.000"),
                new BigDecimal("100.000"),
                CultureType.WHEAT,
                null,
                CREATED, LocalDateTime.of(2026, 4, 14, 10, 0)
        );
    }

    public static SiloResponse fullSilo() {
        return new SiloResponse(
                SILO_ID, COMPANY_A_ID,
                "Silo-A1",
                new BigDecimal("500.000"),
                new BigDecimal("500.000"),
                CultureType.WHEAT,
                null,
                CREATED, LocalDateTime.of(2026, 4, 14, 12, 0)
        );
    }

    public static SiloResponse siloCompanyB() {
        return new SiloResponse(
                SILO_B_ID, COMPANY_B_ID,
                "Silo-B1",
                new BigDecimal("300.000"),
                BigDecimal.ZERO,
                null,
                null,
                CREATED, CREATED
        );
    }
}
