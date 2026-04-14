package com.grainflow.warehouse.service.impl;

import com.grainflow.warehouse.dto.lab.*;
import com.grainflow.warehouse.entity.LabAnalysis;
import com.grainflow.warehouse.entity.LabStatus;
import com.grainflow.warehouse.entity.Vehicle;
import com.grainflow.warehouse.entity.VehicleStatus;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.LabAnalysisMapper;
import com.grainflow.warehouse.repository.LabAnalysisRepository;
import com.grainflow.warehouse.repository.LabAnalysisSpecification;
import com.grainflow.warehouse.repository.VehicleRepository;
import com.grainflow.warehouse.service.LabAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabAnalysisServiceImpl implements LabAnalysisService {

    private final LabAnalysisRepository labAnalysisRepository;
    private final VehicleRepository vehicleRepository;
    private final LabAnalysisMapper labAnalysisMapper;

    @Override
    @Transactional
    public LabAnalysisResponse create(CreateLabAnalysisRequest request, UUID companyId) {
        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> WarehouseException.notFound("Vehicle not found: " + request.vehicleId()));

        if (!vehicle.getCompanyId().equals(companyId)) {
            throw WarehouseException.forbidden("Access denied");
        }
        if (vehicle.getStatus() == VehicleStatus.REJECTED) {
            throw WarehouseException.badRequest("Cannot create lab analysis for a rejected vehicle");
        }
        if (labAnalysisRepository.existsByVehicleId(request.vehicleId())) {
            throw WarehouseException.conflict("Lab analysis already exists for vehicle: " + request.vehicleId());
        }

        LabAnalysis labAnalysis = LabAnalysis.builder()
                .companyId(companyId)
                .vehicle(vehicle)
                .comment(request.comment())
                .build();

        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    @Override
    @Transactional
    public LabAnalysisResponse update(UUID id, UpdateLabAnalysisRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (labAnalysis.getStatus() == LabStatus.PASSED || labAnalysis.getStatus() == LabStatus.FAILED) {
            throw WarehouseException.badRequest("Cannot update a finalized lab analysis");
        }

        labAnalysisMapper.updateFromDto(labAnalysis, request);
        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    @Override
    @Transactional(readOnly = true)
    public LabAnalysisResponse getById(UUID id, UUID companyId) {
        return labAnalysisMapper.toResponseDto(findOwned(id, companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LabAnalysisResponse> getAll(UUID companyId, LabAnalysisFilterRequest filter, Pageable pageable) {
        return labAnalysisRepository
                .findAll(LabAnalysisSpecification.filter(companyId, filter), pageable)
                .map(labAnalysisMapper::toResponseDto);
    }

    @Override
    @Transactional
    public LabAnalysisResponse start(UUID id, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (labAnalysis.getStatus() != LabStatus.PENDING) {
            throw WarehouseException.badRequest("Analysis must be in PENDING status to start");
        }

        labAnalysis.setStatus(LabStatus.IN_PROGRESS);
        labAnalysis.setAnalysisStartedAt(LocalDateTime.now());
        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    @Override
    @Transactional
    public LabAnalysisResponse startDrying(UUID id, StartDryingRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (labAnalysis.getStatus() != LabStatus.IN_PROGRESS) {
            throw WarehouseException.badRequest("Analysis must be in IN_PROGRESS status to start drying");
        }
        if (labAnalysis.getDryingStartedAt() != null) {
            throw WarehouseException.badRequest("Drying has already been started");
        }

        labAnalysis.setVolumeBeforeDrying(request.volumeBeforeDrying());
        labAnalysis.setEstimatedDryingEndAt(request.estimatedDryingEndAt());
        labAnalysis.setDryingStartedAt(LocalDateTime.now());
        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    @Override
    @Transactional
    public LabAnalysisResponse finishDrying(UUID id, FinishDryingRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (labAnalysis.getDryingStartedAt() == null) {
            throw WarehouseException.badRequest("Drying has not been started yet");
        }
        if (labAnalysis.getDryingFinishedAt() != null) {
            throw WarehouseException.badRequest("Drying has already been finished");
        }

        labAnalysis.setVolumeAfterDrying(request.volumeAfterDrying());
        labAnalysis.setMoistureAfterDrying(request.moistureAfterDrying());
        labAnalysis.setDryingFinishedAt(LocalDateTime.now());
        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    @Override
    @Transactional
    public LabAnalysisResponse finishAnalysis(UUID id, FinishAnalysisRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (labAnalysis.getStatus() != LabStatus.IN_PROGRESS) {
            throw WarehouseException.badRequest("Analysis must be in IN_PROGRESS status to finish");
        }

        labAnalysis.setMoisture(request.moisture());
        labAnalysis.setImpurity(request.impurity());
        labAnalysis.setProtein(request.protein());
        labAnalysis.setActualVolume(request.actualVolume());
        labAnalysis.setStatus(request.status());
        labAnalysis.setAnalysisFinishedAt(LocalDateTime.now());
        labAnalysis.setDecidedAt(LocalDateTime.now());
        if (request.comment() != null) labAnalysis.setComment(request.comment());

        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    private LabAnalysis findOwned(UUID id, UUID companyId) {
        LabAnalysis labAnalysis = labAnalysisRepository.findById(id)
                .orElseThrow(() -> WarehouseException.notFound("Lab analysis not found: " + id));
        if (!labAnalysis.getCompanyId().equals(companyId)) {
            throw WarehouseException.forbidden("Access denied");
        }
        return labAnalysis;
    }
}
