package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.batch.AddVolumeRequest;
import com.grainflow.warehouse.dto.batch.BatchResponse;
import com.grainflow.warehouse.dto.batch.CreateBatchRequest;
import com.grainflow.warehouse.dto.batch.UpdateBatchRequest;
import com.grainflow.warehouse.dto.batch.BatchFilterRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BatchService {

    BatchResponse create(CreateBatchRequest request, UUID companyId);

    BatchResponse update(UUID id, UpdateBatchRequest request, UUID companyId);

    void close(UUID id, UUID companyId);

    BatchResponse getById(UUID id, UUID companyId);

    Page<BatchResponse> getAll(UUID companyId, BatchFilterRequest filter, Pageable pageable);

    BatchResponse addAcceptedVolume(UUID id, AddVolumeRequest request, UUID companyId);

    BatchResponse addUnloadedVolume(UUID id, AddVolumeRequest request, UUID companyId);
}
