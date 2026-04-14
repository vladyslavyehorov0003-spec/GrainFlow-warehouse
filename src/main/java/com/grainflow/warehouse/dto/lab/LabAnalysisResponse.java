package com.grainflow.warehouse.dto.lab;

import com.grainflow.warehouse.entity.LabStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record LabAnalysisResponse(

        UUID id,
        UUID companyId,
        UUID vehicleId,
        LabStatus status,

        LocalDateTime analysisStartedAt,
        LocalDateTime analysisFinishedAt,

        BigDecimal moisture,
        BigDecimal impurity,
        BigDecimal protein,

        LocalDateTime dryingStartedAt,
        LocalDateTime dryingFinishedAt,
        LocalDateTime estimatedDryingEndAt,
        BigDecimal volumeBeforeDrying,
        BigDecimal volumeAfterDrying,
        BigDecimal moistureAfterDrying,

        BigDecimal actualVolume,
        LocalDateTime decidedAt,

        UUID siloId,
        LocalDateTime storedAt,

        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
