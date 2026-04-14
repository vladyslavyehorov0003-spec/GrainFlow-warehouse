package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.lab.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface LabAnalysisService {

    LabAnalysisResponse create(CreateLabAnalysisRequest request, UUID companyId);

    LabAnalysisResponse update(UUID id, UpdateLabAnalysisRequest request, UUID companyId);

    LabAnalysisResponse getById(UUID id, UUID companyId);

    Page<LabAnalysisResponse> getAll(UUID companyId, LabAnalysisFilterRequest filter, Pageable pageable);

    LabAnalysisResponse start(UUID id, UUID companyId);

    LabAnalysisResponse startDrying(UUID id, StartDryingRequest request, UUID companyId);

    LabAnalysisResponse finishDrying(UUID id, FinishDryingRequest request, UUID companyId);

    LabAnalysisResponse finishAnalysis(UUID id, FinishAnalysisRequest request, UUID companyId);
}
