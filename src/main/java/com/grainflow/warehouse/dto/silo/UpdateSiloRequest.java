package com.grainflow.warehouse.dto.silo;

import com.grainflow.warehouse.entity.CultureType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateSiloRequest(

        String name,

        @DecimalMin(value = "0.001", message = "Max amount must be greater than 0")
        BigDecimal maxAmount,

        @DecimalMin(value = "0.000", inclusive = true)
        BigDecimal currentAmount,

        CultureType culture,

        String comment
) {}
