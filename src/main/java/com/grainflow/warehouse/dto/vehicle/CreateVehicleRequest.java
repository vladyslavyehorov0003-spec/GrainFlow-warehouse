package com.grainflow.warehouse.dto.vehicle;

import com.grainflow.warehouse.entity.CultureType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateVehicleRequest(

        @NotNull(message = "Batch ID is required")
        UUID batchId,

        @NotBlank(message = "License plate is required")
        String licensePlate,

        String driverName,

        @NotNull(message = "Culture is required")
        CultureType culture,

        @NotNull(message = "Declared volume is required")
        @DecimalMin(value = "0.001", message = "Declared volume must be greater than 0")
        BigDecimal declaredVolume,

        // Optional — defaults to now in service
        LocalDateTime arrivedAt,

        String comment
) {}
