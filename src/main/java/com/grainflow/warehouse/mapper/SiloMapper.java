package com.grainflow.warehouse.mapper;

import com.grainflow.warehouse.dto.silo.SiloResponse;
import com.grainflow.warehouse.dto.silo.UpdateSiloRequest;
import com.grainflow.warehouse.entity.Silo;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SiloMapper {

    SiloResponse toResponseDto(Silo silo);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(@MappingTarget Silo silo, UpdateSiloRequest request);
}
