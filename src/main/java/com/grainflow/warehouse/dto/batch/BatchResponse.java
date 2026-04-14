package com.grainflow.warehouse.dto.batch;

import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.entity.CultureType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record BatchResponse(

        UUID id,
        UUID companyId,
        String contractNumber,
        CultureType culture,
        BatchStatus status,
        BigDecimal totalVolume,
        BigDecimal acceptedVolume,
        BigDecimal unloadedVolume,
        LocalDate loadingFrom,
        LocalDate loadingTo,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
