package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.vehicle.CreateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.UpdateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleFilterRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VehicleService {

    VehicleResponse create(CreateVehicleRequest request, UUID companyId);

    VehicleResponse update(UUID id, UpdateVehicleRequest request, UUID companyId);

    VehicleResponse getById(UUID id, UUID companyId);

    Page<VehicleResponse> getAll(UUID companyId, VehicleFilterRequest filter, Pageable pageable);

    VehicleResponse startProcessing(UUID id, UUID companyId);

    VehicleResponse finishProcessing(UUID id, UUID companyId);

    VehicleResponse accept(UUID id, UUID companyId);

    VehicleResponse reject(UUID id, String comment, UUID companyId);
}
