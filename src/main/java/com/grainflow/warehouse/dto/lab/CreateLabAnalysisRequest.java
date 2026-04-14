package com.grainflow.warehouse.dto.lab;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLabAnalysisRequest(

        @NotNull(message = "Vehicle ID is required")
        UUID vehicleId,

        String comment
) {}
