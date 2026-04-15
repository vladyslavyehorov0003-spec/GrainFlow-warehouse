package com.grainflow.warehouse.service.impl;

import com.grainflow.warehouse.dto.vehicle.CreateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.UpdateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleFilterRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleResponse;
import com.grainflow.warehouse.entity.Batch;
import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.entity.Vehicle;
import com.grainflow.warehouse.entity.VehicleStatus;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.VehicleMapper;
import com.grainflow.warehouse.repository.BatchRepository;
import com.grainflow.warehouse.repository.VehicleRepository;
import com.grainflow.warehouse.repository.VehicleSpecification;
import com.grainflow.warehouse.audit.Auditable;
import com.grainflow.warehouse.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BatchRepository batchRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    @Transactional
    @Auditable(action = "VEHICLE_CREATED", entityType = "VEHICLE")
    public VehicleResponse create(CreateVehicleRequest request, UUID companyId) {
        Batch batch = batchRepository.findById(request.batchId())
                .orElseThrow(() -> WarehouseException.notFound("Batch not found: " + request.batchId()));

        if (!batch.getCompanyId().equals(companyId)) {
            throw WarehouseException.forbidden("Access denied");
        }
        if (batch.getStatus() == BatchStatus.CLOSED) {
            throw WarehouseException.badRequest("Cannot register vehicle for a closed batch");
        }

        Vehicle vehicle = Vehicle.builder()
                .companyId(companyId)
                .batch(batch)
                .licensePlate(request.licensePlate())
                .driverName(request.driverName())
                .culture(request.culture())
                .declaredVolume(request.declaredVolume())
                .arrivedAt(request.arrivedAt() != null ? request.arrivedAt() : LocalDateTime.now())
                .comment(request.comment())
                .build();

        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    @Auditable(action = "VEHICLE_UPDATED", entityType = "VEHICLE")
    public VehicleResponse update(UUID id, UpdateVehicleRequest request, UUID companyId) {
        Vehicle vehicle = findOwned(id, companyId);

        if (vehicle.getStatus() == VehicleStatus.ACCEPTED || vehicle.getStatus() == VehicleStatus.REJECTED) {
            throw WarehouseException.badRequest("Cannot update a finalized vehicle");
        }

        vehicleMapper.updateVehicleFromDto(vehicle, request);
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getById(UUID id, UUID companyId) {
        return vehicleMapper.toResponseDto(findOwned(id, companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleResponse> getAll(UUID companyId, VehicleFilterRequest filter, Pageable pageable) {
        return vehicleRepository
                .findAll(VehicleSpecification.filter(companyId, filter), pageable)
                .map(vehicleMapper::toResponseDto);
    }

    @Override
    @Transactional
    @Auditable(action = "VEHICLE_PROCESSING_STARTED", entityType = "VEHICLE")
    public VehicleResponse startProcessing(UUID id, UUID companyId) {
        Vehicle vehicle = findOwned(id, companyId);

        if (vehicle.getStatus() != VehicleStatus.ARRIVED) {
            throw WarehouseException.badRequest("Vehicle must be in ARRIVED status to start processing");
        }

        vehicle.setStatus(VehicleStatus.IN_PROCESS);
        vehicle.setUnloadingStartedAt(LocalDateTime.now());
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    @Auditable(action = "VEHICLE_PROCESSING_FINISHED", entityType = "VEHICLE")
    public VehicleResponse finishProcessing(UUID id, UUID companyId) {
        Vehicle vehicle = findOwned(id, companyId);

        if (vehicle.getStatus() != VehicleStatus.IN_PROCESS) {
            throw WarehouseException.badRequest("Vehicle must be in IN_PROCESS status to finish processing");
        }

        vehicle.setUnloadingFinishedAt(LocalDateTime.now());
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    @Auditable(action = "VEHICLE_ACCEPTED", entityType = "VEHICLE")
    public VehicleResponse accept(UUID id, UUID companyId) {
        Vehicle vehicle = findOwned(id, companyId);

        if (vehicle.getStatus() != VehicleStatus.IN_PROCESS) {
            throw WarehouseException.badRequest("Vehicle must be in IN_PROCESS status to be accepted");
        }

        vehicle.setStatus(VehicleStatus.ACCEPTED);
        vehicle.setDecidedAt(LocalDateTime.now());
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional
    @Auditable(action = "VEHICLE_REJECTED", entityType = "VEHICLE")
    public VehicleResponse reject(UUID id, String comment, UUID companyId) {
        Vehicle vehicle = findOwned(id, companyId);

        if (vehicle.getStatus() != VehicleStatus.IN_PROCESS) {
            throw WarehouseException.badRequest("Vehicle must be in IN_PROCESS status to be rejected");
        }

        vehicle.setStatus(VehicleStatus.REJECTED);
        vehicle.setDecidedAt(LocalDateTime.now());
        if (comment != null) vehicle.setComment(comment);
        return vehicleMapper.toResponseDto(vehicleRepository.save(vehicle));
    }

    private Vehicle findOwned(UUID id, UUID companyId) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> WarehouseException.notFound("Vehicle not found: " + id));
        if (!vehicle.getCompanyId().equals(companyId)) {
            throw WarehouseException.forbidden("Access denied");
        }
        return vehicle;
    }
}
