package com.grainflow.warehouse.dto.lab;

import com.grainflow.warehouse.entity.LabStatus;

import java.util.UUID;

public record LabAnalysisFilterRequest(

        UUID vehicleId,
        UUID batchId,
        LabStatus status
) {}
