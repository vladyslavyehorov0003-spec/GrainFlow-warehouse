package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.entity.LabAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface LabAnalysisRepository extends JpaRepository<LabAnalysis, UUID>, JpaSpecificationExecutor<LabAnalysis> {

    boolean existsByVehicleId(UUID vehicleId);
}
