package com.grainflow.warehouse.dto.batch;

import com.grainflow.warehouse.entity.CultureType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBatchRequest(

        @NotBlank(message = "Contract number is required")
        String contractNumber,

        @NotNull(message = "Culture is required")
        CultureType culture,

        @NotNull(message = "Total volume is required")
        @DecimalMin(value = "0.001", message = "Total volume must be greater than 0")
        BigDecimal totalVolume,

        @NotNull(message = "Loading from date is required")
        LocalDate loadingFrom,

        @NotNull(message = "Loading to date is required")
        LocalDate loadingTo,

        String comment
) {}
