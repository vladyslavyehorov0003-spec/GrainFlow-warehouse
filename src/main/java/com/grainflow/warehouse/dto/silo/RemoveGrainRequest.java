package com.grainflow.warehouse.dto.silo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RemoveGrainRequest(

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.001", message = "Amount must be greater than 0")
        BigDecimal amount
) {}
