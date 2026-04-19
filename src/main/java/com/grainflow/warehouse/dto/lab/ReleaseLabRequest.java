package com.grainflow.warehouse.dto.lab;

import com.grainflow.warehouse.entity.LabStatus;
import jakarta.validation.constraints.NotNull;

public record ReleaseLabRequest(

        // Must be PASSED or FAILED
        @NotNull(message = "Status is required")
        boolean isApproved,

        String comment
) {}
