package com.grainflow.warehouse.mapper;

import com.grainflow.warehouse.dto.lab.LabAnalysisResponse;
import com.grainflow.warehouse.dto.lab.UpdateLabAnalysisRequest;
import com.grainflow.warehouse.entity.LabAnalysis;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LabAnalysisMapper {

    @Mapping(source = "vehicle.id", target = "vehicleId")
    LabAnalysisResponse toResponseDto(LabAnalysis labAnalysis);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(@MappingTarget LabAnalysis labAnalysis, UpdateLabAnalysisRequest request);
}
