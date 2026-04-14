package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.entity.LabAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LabAnalysisRepository extends JpaRepository<LabAnalysis, UUID> {
}
