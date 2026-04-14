package com.grainflow.warehouse.dto.vehicle;

import com.grainflow.warehouse.entity.CultureType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// All fields optional — manager corrects only what's wrong
public record UpdateVehicleRequest(

        String licensePlate,
        String driverName,
        CultureType culture,

        @DecimalMin(value = "0.001", message = "Declared volume must be greater than 0")
        BigDecimal declaredVolume,

        LocalDateTime arrivedAt,
        LocalDateTime unloadingStartedAt,
        LocalDateTime unloadingFinishedAt,
        LocalDateTime decidedAt,

        String comment
) {}
