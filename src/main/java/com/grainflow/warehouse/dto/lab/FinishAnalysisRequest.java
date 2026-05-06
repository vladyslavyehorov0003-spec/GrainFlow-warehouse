package com.grainflow.warehouse.dto.lab;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinishAnalysisRequest(

        @NotNull(message = "Moisture is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Moisture must be at least 0%")
        @DecimalMax(value = "100.00", inclusive = true, message = "Moisture cannot exceed 100%")
        BigDecimal moisture,

        @NotNull(message = "Impurity is required")
        @DecimalMin(value = "0.00", inclusive = true, message = "Impurity must be at least 0%")
        @DecimalMax(value = "100.00", inclusive = true, message = "Impurity cannot exceed 100%")
        BigDecimal impurity,

        // Optional — not all cultures require protein measurement
        @DecimalMin(value = "0.00", inclusive = true, message = "Protein must be at least 0%")
        @DecimalMax(value = "100.00", inclusive = true, message = "Protein cannot exceed 100%")
        BigDecimal protein,

        @NotNull(message = "Actual volume is required")
        @DecimalMin(value = "0.001", message = "Actual volume must be greater than 0")
        BigDecimal actualVolume,

        String comment
) {}
