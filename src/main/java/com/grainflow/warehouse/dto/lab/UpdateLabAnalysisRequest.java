package com.grainflow.warehouse.dto.lab;

import com.grainflow.warehouse.entity.LabStatus;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// All fields optional — manager corrects only what's wrong
public record UpdateLabAnalysisRequest(

        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal moisture,

        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal impurity,

        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal protein,

        @DecimalMin(value = "0.001")
        BigDecimal volumeBeforeDrying,

        @DecimalMin(value = "0.001")
        BigDecimal volumeAfterDrying,

        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal moistureAfterDrying,

        @DecimalMin(value = "0.001")
        BigDecimal actualVolume,

        LocalDateTime analysisStartedAt,
        LocalDateTime analysisFinishedAt,
        LocalDateTime dryingStartedAt,
        LocalDateTime dryingFinishedAt,
        LocalDateTime decidedAt,

        boolean approved,

        LabStatus status,
        String comment
) {}
