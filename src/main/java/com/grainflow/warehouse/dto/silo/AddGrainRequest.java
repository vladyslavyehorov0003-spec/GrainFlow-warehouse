package com.grainflow.warehouse.dto.silo;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddGrainRequest(

        @NotNull(message = "Lab analysis ID is required")
        UUID labAnalysisId
) {}
