package com.grainflow.warehouse.dto.silo;

import com.grainflow.warehouse.entity.CultureType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SiloResponse(

        UUID id,
        UUID companyId,
        String name,
        BigDecimal maxAmount,
        BigDecimal currentAmount,
        CultureType culture,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
