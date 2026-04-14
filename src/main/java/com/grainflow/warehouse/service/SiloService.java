package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.silo.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SiloService {

    SiloResponse create(CreateSiloRequest request, UUID companyId);

    SiloResponse update(UUID id, UpdateSiloRequest request, UUID companyId);

    void delete(UUID id, UUID companyId);

    SiloResponse getById(UUID id, UUID companyId);

    Page<SiloResponse> getAll(UUID companyId, SiloFilterRequest filter, Pageable pageable);

    SiloResponse addGrain(UUID id, AddGrainRequest request, UUID companyId);

    SiloResponse removeGrain(UUID id, RemoveGrainRequest request, UUID companyId);
}
