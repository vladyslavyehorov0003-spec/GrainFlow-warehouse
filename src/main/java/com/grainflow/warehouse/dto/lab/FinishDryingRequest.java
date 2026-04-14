package com.grainflow.warehouse.dto.lab;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinishDryingRequest(

        @NotNull(message = "Volume after drying is required")
        @DecimalMin(value = "0.001", message = "Volume must be greater than 0")
        BigDecimal volumeAfterDrying,

        @NotNull(message = "Moisture after drying is required")
        @DecimalMin(value = "0.00", inclusive = true)
        BigDecimal moistureAfterDrying
) {}
