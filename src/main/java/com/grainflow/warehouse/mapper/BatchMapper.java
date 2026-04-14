package com.grainflow.warehouse.mapper;

import com.grainflow.warehouse.dto.batch.BatchResponse;
import com.grainflow.warehouse.dto.batch.CreateBatchRequest;
import com.grainflow.warehouse.dto.batch.UpdateBatchRequest;
import com.grainflow.warehouse.entity.Batch;
import org.mapstruct.*;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface BatchMapper {

    @Mapping(source = "companyId", target = "companyId")
    Batch toEntity(CreateBatchRequest request, UUID companyId);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateBatchFromDto(@MappingTarget Batch batch, UpdateBatchRequest request);

    BatchResponse toResponseDto(Batch batch);
}
