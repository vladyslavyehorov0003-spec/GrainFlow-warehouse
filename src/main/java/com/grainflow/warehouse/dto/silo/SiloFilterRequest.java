package com.grainflow.warehouse.dto.silo;

import com.grainflow.warehouse.entity.CultureType;

public record SiloFilterRequest(

        String name,
        CultureType culture
) {}
