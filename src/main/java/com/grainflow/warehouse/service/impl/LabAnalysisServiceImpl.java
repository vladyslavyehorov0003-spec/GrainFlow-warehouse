package com.grainflow.warehouse.service.impl;

import com.grainflow.warehouse.dto.lab.*;
import com.grainflow.warehouse.entity.ApprovalStatus;
import com.grainflow.warehouse.entity.LabAnalysis;
import com.grainflow.warehouse.entity.LabStatus;
import com.grainflow.warehouse.entity.Vehicle;
import com.grainflow.warehouse.entity.VehicleStatus;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.LabAnalysisMapper;
import com.grainflow.warehouse.repository.LabAnalysisRepository;
import com.grainflow.warehouse.repository.LabAnalysisSpecification;
import com.grainflow.warehouse.repository.VehicleRepository;
import com.grainflow.warehouse.audit.Auditable;
import com.grainflow.warehouse.service.LabAnalysisService;
import com.grainflow.warehouse.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabAnalysisServiceImpl implements LabAnalysisService {

    private final LabAnalysisRepository labAnalysisRepository;
    private final VehicleRepository vehicleRepository;
    private final LabAnalysisMapper labAnalysisMapper;

    @Lazy
    @Autowired
    private VehicleService vehicleService;

    @Override
    @Transactional
    @Auditable(action = "LAB_CREATED", entityType = "LAB_ANALYSIS")
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
    @Auditable(action = "LAB_UPDATED", entityType = "LAB_ANALYSIS")
    public LabAnalysisResponse update(UUID id, UpdateLabAnalysisRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);


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

    // PENDING → IN_PROGRESS
    @Override
    @Transactional
    @Auditable(action = "LAB_STARTED", entityType = "LAB_ANALYSIS")
    public LabAnalysisResponse start(UUID id, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (labAnalysis.getStatus() != LabStatus.PENDING) {
            throw WarehouseException.badRequest("Analysis must be in PENDING status to start");
        }

        labAnalysis.setStatus(LabStatus.IN_PROGRESS);
        labAnalysis.setAnalysisStartedAt(LocalDateTime.now());
        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    // IN_PROGRESS → ANALYSIS_DONE  (record measurements)
    @Override
    @Transactional
    @Auditable(action = "LAB_ANALYSIS_FINISHED", entityType = "LAB_ANALYSIS")
    public LabAnalysisResponse finishAnalysis(UUID id, FinishAnalysisRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (labAnalysis.getStatus() != LabStatus.IN_PROGRESS) {
            throw WarehouseException.badRequest("Analysis must be in IN_PROGRESS status to finish");
        }

        labAnalysis.setMoisture(request.moisture());
        labAnalysis.setImpurity(request.impurity());
        labAnalysis.setProtein(request.protein());
        labAnalysis.setActualVolume(request.actualVolume());
        labAnalysis.setStatus(LabStatus.ANALYSIS_DONE);
        labAnalysis.setAnalysisFinishedAt(LocalDateTime.now());
        if (request.comment() != null) labAnalysis.setComment(request.comment());

        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    // ANALYSIS_DONE → DRYING
    @Override
    @Transactional
    @Auditable(action = "LAB_DRYING_STARTED", entityType = "LAB_ANALYSIS")
    public LabAnalysisResponse startDrying(UUID id, StartDryingRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (request.volumeBeforeDrying().compareTo(labAnalysis.getActualVolume()) > 0) {
            throw WarehouseException.badRequest("Volume before drying cannot exceed actual volume");
        }
        if (labAnalysis.getStatus() != LabStatus.ANALYSIS_DONE) {
            throw WarehouseException.badRequest("Analysis must be in ANALYSIS_DONE status to start drying");
        }
        if (labAnalysis.getVehicle().getStatus() != VehicleStatus.ACCEPTED){
            throw WarehouseException.badRequest("Vehicle must be in ACCEPTED status to start drying");
        }

        labAnalysis.setStatus(LabStatus.DRYING);
        labAnalysis.setVolumeBeforeDrying(request.volumeBeforeDrying());
        labAnalysis.setEstimatedDryingEndAt(request.estimatedDryingEndAt());
        labAnalysis.setDryingStartedAt(LocalDateTime.now());
        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    // DRYING → DRYING_DONE
    @Override
    @Transactional
    @Auditable(action = "LAB_DRYING_FINISHED", entityType = "LAB_ANALYSIS")
    public LabAnalysisResponse finishDrying(UUID id, FinishDryingRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        if (request.volumeAfterDrying().compareTo(labAnalysis.getVolumeBeforeDrying()) > 0) {
            throw WarehouseException.badRequest("Volume after drying cannot exceed volume before drying");
        }
        if (labAnalysis.getStatus() != LabStatus.DRYING) {
            throw WarehouseException.badRequest("Analysis must be in DRYING status to finish drying");
        }

        labAnalysis.setStatus(LabStatus.DRYING_DONE);
        labAnalysis.setVolumeAfterDrying(request.volumeAfterDrying());
        labAnalysis.setMoistureAfterDrying(request.moistureAfterDrying());
        labAnalysis.setDryingFinishedAt(LocalDateTime.now());
        return labAnalysisMapper.toResponseDto(labAnalysisRepository.save(labAnalysis));
    }

    // ANALYSIS_DONE | DRYING | DRYING_DONE → PASSED/FAILED  +  vehicle → PENDING_REVIEW
    // "Release vehicle for manager review" — can be called at any point after analysis is recorded
    @Override
    @Transactional
    @Auditable(action = "LAB_RELEASED", entityType = "LAB_ANALYSIS")
    public LabAnalysisResponse release(UUID id, ReleaseLabRequest request, UUID companyId) {
        LabAnalysis labAnalysis = findOwned(id, companyId);

        Set<LabStatus> releasable = Set.of(
                LabStatus.ANALYSIS_DONE,
                LabStatus.DRYING,
                LabStatus.DRYING_DONE
        );
        if (!releasable.contains(labAnalysis.getStatus())) {
            throw WarehouseException.badRequest(
                    "Cannot release vehicle: analysis must be in ANALYSIS_DONE, DRYING, or DRYING_DONE status"
            );
        }


        labAnalysis.setApprovalStatus(request.isApproved() ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        labAnalysis.setDecidedAt(LocalDateTime.now());
        if (request.comment() != null) labAnalysis.setComment(request.comment());
        labAnalysisRepository.save(labAnalysis);

        // Move vehicle to PENDING_REVIEW so manager can accept or reject
        vehicleService.finishProcessing(labAnalysis.getVehicle().getId(), companyId);

        return labAnalysisMapper.toResponseDto(labAnalysis);
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
