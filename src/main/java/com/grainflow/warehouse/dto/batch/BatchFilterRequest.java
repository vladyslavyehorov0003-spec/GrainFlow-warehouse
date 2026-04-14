package com.grainflow.warehouse.dto.batch;

import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.entity.CultureType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// Query parameters for GET /batches — all fields optional
public record BatchFilterRequest(

        String contractNumber,
        CultureType culture,
        BatchStatus status,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate loadingFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate loadingTo
) {}
