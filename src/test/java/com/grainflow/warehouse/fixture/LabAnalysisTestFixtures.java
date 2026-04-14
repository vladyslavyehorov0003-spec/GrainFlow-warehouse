package com.grainflow.warehouse.fixture;

import com.grainflow.warehouse.dto.lab.LabAnalysisResponse;
import com.grainflow.warehouse.entity.LabStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.grainflow.warehouse.fixture.BatchTestFixtures.COMPANY_A_ID;
import static com.grainflow.warehouse.fixture.VehicleTestFixtures.VEHICLE_ID;

public class LabAnalysisTestFixtures {

    public static final UUID LAB_ID  = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    public static final UUID SILO_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 4, 14, 8, 0);

    public static LabAnalysisResponse pendingLab() {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A_ID)
                .vehicleId(VEHICLE_ID)
                .status(LabStatus.PENDING)
                .createdAt(CREATED)
                .updatedAt(CREATED)
                .build();
    }

    public static LabAnalysisResponse inProgressLab() {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A_ID)
                .vehicleId(VEHICLE_ID)
                .status(LabStatus.IN_PROGRESS)
                .analysisStartedAt(LocalDateTime.of(2026, 4, 14, 9, 0))
                .createdAt(CREATED)
                .updatedAt(LocalDateTime.of(2026, 4, 14, 9, 0))
                .build();
    }

    public static LabAnalysisResponse dryingLab() {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A_ID)
                .vehicleId(VEHICLE_ID)
                .status(LabStatus.IN_PROGRESS)
                .analysisStartedAt(LocalDateTime.of(2026, 4, 14, 9, 0))
                .dryingStartedAt(LocalDateTime.of(2026, 4, 14, 10, 0))
                .estimatedDryingEndAt(LocalDateTime.of(2026, 4, 14, 12, 0))
                .volumeBeforeDrying(new BigDecimal("25.500"))
                .createdAt(CREATED)
                .updatedAt(LocalDateTime.of(2026, 4, 14, 10, 0))
                .build();
    }

    public static LabAnalysisResponse passedLab() {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A_ID)
                .vehicleId(VEHICLE_ID)
                .status(LabStatus.PASSED)
                .analysisStartedAt(LocalDateTime.of(2026, 4, 14, 9, 0))
                .analysisFinishedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .moisture(new BigDecimal("14.50"))
                .impurity(new BigDecimal("1.20"))
                .protein(new BigDecimal("12.00"))
                .dryingStartedAt(LocalDateTime.of(2026, 4, 14, 10, 0))
                .dryingFinishedAt(LocalDateTime.of(2026, 4, 14, 12, 0))
                .volumeBeforeDrying(new BigDecimal("25.500"))
                .volumeAfterDrying(new BigDecimal("24.800"))
                .moistureAfterDrying(new BigDecimal("13.00"))
                .actualVolume(new BigDecimal("24.800"))
                .decidedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .createdAt(CREATED)
                .updatedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .build();
    }

    public static LabAnalysisResponse failedLab() {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A_ID)
                .vehicleId(VEHICLE_ID)
                .status(LabStatus.FAILED)
                .analysisStartedAt(LocalDateTime.of(2026, 4, 14, 9, 0))
                .analysisFinishedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .moisture(new BigDecimal("28.50"))
                .impurity(new BigDecimal("5.00"))
                .actualVolume(new BigDecimal("25.500"))
                .decidedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .comment("moisture too high")
                .createdAt(CREATED)
                .updatedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .build();
    }

    public static LabAnalysisResponse storedLab() {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A_ID)
                .vehicleId(VEHICLE_ID)
                .status(LabStatus.STORED)
                .analysisStartedAt(LocalDateTime.of(2026, 4, 14, 9, 0))
                .analysisFinishedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .moisture(new BigDecimal("14.50"))
                .impurity(new BigDecimal("1.20"))
                .actualVolume(new BigDecimal("24.800"))
                .decidedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .siloId(SILO_ID)
                .storedAt(LocalDateTime.of(2026, 4, 14, 14, 0))
                .createdAt(CREATED)
                .updatedAt(LocalDateTime.of(2026, 4, 14, 14, 0))
                .build();
    }
}
