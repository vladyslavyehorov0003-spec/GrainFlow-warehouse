package com.grainflow.warehouse.dto.lab;

import com.grainflow.warehouse.entity.LabStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// All fields optional — manager corrects only what's wrong
public record UpdateLabAnalysisRequest(

        @DecimalMin(value = "0.00", inclusive = true, message = "Moisture must be at least 0%")
        @DecimalMax(value = "100.00", inclusive = true, message = "Moisture cannot exceed 100%")
        BigDecimal moisture,

        @DecimalMin(value = "0.00", inclusive = true, message = "Impurity must be at least 0%")
        @DecimalMax(value = "100.00", inclusive = true, message = "Impurity cannot exceed 100%")
        BigDecimal impurity,

        @DecimalMin(value = "0.00", inclusive = true, message = "Protein must be at least 0%")
        @DecimalMax(value = "100.00", inclusive = true, message = "Protein cannot exceed 100%")
        BigDecimal protein,

        @DecimalMin(value = "0.001")
        BigDecimal volumeBeforeDrying,

        @DecimalMin(value = "0.001")
        BigDecimal volumeAfterDrying,

        @DecimalMin(value = "0.00", inclusive = true, message = "Moisture must be at least 0%")
        @DecimalMax(value = "100.00", inclusive = true, message = "Moisture cannot exceed 100%")
        BigDecimal moistureAfterDrying,

        @DecimalMin(value = "0.001")
        BigDecimal actualVolume,

        LocalDateTime analysisStartedAt,
        LocalDateTime analysisFinishedAt,
        LocalDateTime dryingStartedAt,
        LocalDateTime dryingFinishedAt,
        LocalDateTime decidedAt,

        Boolean approved,

        LabStatus status,
        String comment
) {}
