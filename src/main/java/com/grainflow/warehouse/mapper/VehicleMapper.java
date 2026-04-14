package com.grainflow.warehouse.mapper;

import com.grainflow.warehouse.dto.vehicle.UpdateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleResponse;
import com.grainflow.warehouse.entity.Vehicle;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(source = "batch.id", target = "batchId")
    VehicleResponse toResponseDto(Vehicle vehicle);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateVehicleFromDto(@MappingTarget Vehicle vehicle, UpdateVehicleRequest request);
}
