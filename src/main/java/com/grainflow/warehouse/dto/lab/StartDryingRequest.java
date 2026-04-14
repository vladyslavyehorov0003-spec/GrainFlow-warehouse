package com.grainflow.warehouse.dto.lab;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StartDryingRequest(

        @NotNull(message = "Volume before drying is required")
        @DecimalMin(value = "0.001", message = "Volume must be greater than 0")
        BigDecimal volumeBeforeDrying,

        // Estimated time when drying will finish
        LocalDateTime estimatedDryingEndAt
) {}
