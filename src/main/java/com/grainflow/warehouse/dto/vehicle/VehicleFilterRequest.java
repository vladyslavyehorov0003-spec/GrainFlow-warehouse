package com.grainflow.warehouse.dto.vehicle;

import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.entity.VehicleStatus;

import java.util.UUID;

public record VehicleFilterRequest(

        UUID batchId,
        VehicleStatus status,
        CultureType culture,
        String licensePlate
) {}
