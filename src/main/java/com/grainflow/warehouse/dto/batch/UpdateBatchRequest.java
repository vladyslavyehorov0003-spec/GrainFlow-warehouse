package com.grainflow.warehouse.dto.batch;

import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.entity.CultureType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDate;

// All fields optional — manager corrects only what's wrong
public record UpdateBatchRequest(

        String contractNumber,

        CultureType culture,
        BatchStatus status,

        @DecimalMin(value = "0.001", message = "Total volume must be greater than 0")
        BigDecimal totalVolume,

        @DecimalMin(value = "0.000", inclusive = true, message = "Accepted volume cannot be negative")
        BigDecimal acceptedVolume,

        @DecimalMin(value = "0.000", inclusive = true, message = "Unloaded volume cannot be negative")
        BigDecimal unloadedVolume,


        LocalDate loadingFrom,

        LocalDate loadingTo,

        String comment
) {}
