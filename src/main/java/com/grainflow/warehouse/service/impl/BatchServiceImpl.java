package com.grainflow.warehouse.service.impl;

import com.grainflow.warehouse.dto.batch.AddVolumeRequest;
import com.grainflow.warehouse.dto.batch.BatchFilterRequest;
import com.grainflow.warehouse.dto.batch.BatchResponse;
import com.grainflow.warehouse.dto.batch.CreateBatchRequest;
import com.grainflow.warehouse.dto.batch.UpdateBatchRequest;
import com.grainflow.warehouse.entity.Batch;
import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.BatchMapper;
import com.grainflow.warehouse.repository.BatchRepository;
import com.grainflow.warehouse.repository.BatchSpecification;
import com.grainflow.warehouse.service.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final BatchMapper batchMapper;

    @Override
    @Transactional
    public BatchResponse create(CreateBatchRequest request, UUID companyId) {
        if (request.loadingTo().isBefore(request.loadingFrom())) {
            throw WarehouseException.badRequest("loadingTo must be after loadingFrom");
        }
        if (batchRepository.existsByContractNumberAndCompanyId(request.contractNumber(), companyId)) {
            throw WarehouseException.conflict("Contract number already exists: " + request.contractNumber());
        }

        Batch batch = batchMapper.toEntity(request, companyId);
        return batchMapper.toResponseDto(batchRepository.save(batch));
    }

    @Override
    @Transactional
    public BatchResponse update(UUID id, UpdateBatchRequest request, UUID companyId) {
        Batch batch = findOwned(id, companyId);

        if (batch.getStatus() == BatchStatus.CLOSED) {
            throw WarehouseException.badRequest("Cannot update a closed batch");
        }
        if (request.loadingFrom() != null && request.loadingTo() != null
                && request.loadingTo().isBefore(request.loadingFrom())) {
            throw WarehouseException.badRequest("loadingTo must be after loadingFrom");
        }
        if (request.contractNumber() != null
                && !request.contractNumber().equals(batch.getContractNumber())
                && batchRepository.existsByContractNumberAndCompanyId(request.contractNumber(), companyId)) {
            throw WarehouseException.conflict("Contract number already exists: " + request.contractNumber());
        }

        batchMapper.updateBatchFromDto(batch, request);
        return batchMapper.toResponseDto(batchRepository.save(batch));
    }

    @Override
    @Transactional
    public void close(UUID id, UUID companyId) {
        Batch batch = findOwned(id, companyId);

        if (batch.getStatus() == BatchStatus.CLOSED) {
            throw WarehouseException.badRequest("Batch is already closed");
        }

        batch.setStatus(BatchStatus.CLOSED);
        batchRepository.save(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public BatchResponse getById(UUID id, UUID companyId) {
        return batchMapper.toResponseDto(findOwned(id, companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchResponse> getAll(UUID companyId, BatchFilterRequest filter, Pageable pageable) {
        return batchRepository
                .findAll(BatchSpecification.filter(companyId, filter), pageable)
                .map(batchMapper::toResponseDto);
    }

    @Override
    @Transactional
    public BatchResponse addAcceptedVolume(UUID id, AddVolumeRequest request, UUID companyId) {
        Batch batch = findOwned(id, companyId);

        if (batch.getStatus() == BatchStatus.CLOSED) {
            throw WarehouseException.badRequest("Cannot add volume to a closed batch");
        }

        batch.setAcceptedVolume(batch.getAcceptedVolume().add(request.amount()));

        // Auto-activate if still planned
        if (batch.getStatus() == BatchStatus.PLANNED) {
            batch.setStatus(BatchStatus.ACTIVE);
        }

        return batchMapper.toResponseDto(batchRepository.save(batch));
    }

    @Override
    @Transactional
    public BatchResponse addUnloadedVolume(UUID id, AddVolumeRequest request, UUID companyId) {
        Batch batch = findOwned(id, companyId);

        if (batch.getStatus() == BatchStatus.CLOSED) {
            throw WarehouseException.badRequest("Cannot add volume to a closed batch");
        }

        BigDecimal newUnloaded = batch.getUnloadedVolume().add(request.amount());
        if (newUnloaded.compareTo(batch.getAcceptedVolume()) > 0) {
            throw WarehouseException.badRequest("Unloaded volume cannot exceed accepted volume");
        }

        batch.setUnloadedVolume(newUnloaded);
        return batchMapper.toResponseDto(batchRepository.save(batch));
    }

    // Finds batch by id, throws 404 if not found, 403 if belongs to another company
    private Batch findOwned(UUID id, UUID companyId) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> WarehouseException.notFound("Batch not found: " + id));
        if (!batch.getCompanyId().equals(companyId)) {
            throw WarehouseException.forbidden("Access denied");
        }
        return batch;
    }
}
