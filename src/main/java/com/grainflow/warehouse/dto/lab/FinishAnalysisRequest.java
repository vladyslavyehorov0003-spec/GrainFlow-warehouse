package com.grainflow.warehouse.dto.lab;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinishAnalysisRequest(

        @NotNull(message = "Moisture is required")
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal moisture,

        @NotNull(message = "Impurity is required")
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal impurity,

        // Optional — not all cultures require protein measurement
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal protein,

        @NotNull(message = "Actual volume is required")
        @DecimalMin(value = "0.001", message = "Actual volume must be greater than 0")
        BigDecimal actualVolume,

        String comment
) {}
