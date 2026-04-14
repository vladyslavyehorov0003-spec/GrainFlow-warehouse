package com.grainflow.warehouse.dto.silo;

import com.grainflow.warehouse.entity.CultureType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateSiloRequest(

        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Max amount is required")
        @DecimalMin(value = "0.001", message = "Max amount must be greater than 0")
        BigDecimal maxAmount,

        // Culture can be set upfront or left null until first grain arrives
        CultureType culture,

        String comment
) {}
