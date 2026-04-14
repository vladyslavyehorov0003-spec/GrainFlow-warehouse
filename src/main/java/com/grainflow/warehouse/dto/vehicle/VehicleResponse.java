package com.grainflow.warehouse.dto.vehicle;

import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.entity.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleResponse(

        UUID id,
        UUID companyId,
        UUID batchId,
        String licensePlate,
        String driverName,
        CultureType culture,
        BigDecimal declaredVolume,
        VehicleStatus status,

        LocalDateTime arrivedAt,
        LocalDateTime unloadingStartedAt,
        LocalDateTime unloadingFinishedAt,
        LocalDateTime decidedAt,

        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
