package com.grainflow.warehouse.service.impl;

import com.grainflow.warehouse.dto.silo.*;
import com.grainflow.warehouse.entity.LabAnalysis;
import com.grainflow.warehouse.entity.LabStatus;
import com.grainflow.warehouse.entity.Silo;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.SiloMapper;
import com.grainflow.warehouse.repository.LabAnalysisRepository;
import com.grainflow.warehouse.repository.SiloRepository;
import com.grainflow.warehouse.repository.SiloSpecification;
import com.grainflow.warehouse.service.SiloService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SiloServiceImpl implements SiloService {

    private final SiloRepository siloRepository;
    private final LabAnalysisRepository labAnalysisRepository;
    private final SiloMapper siloMapper;

    @Override
    @Transactional
    public SiloResponse create(CreateSiloRequest request, UUID companyId) {
        if (siloRepository.existsByNameAndCompanyId(request.name(), companyId)) {
            throw WarehouseException.conflict("Silo with this name already exists: " + request.name());
        }

        Silo silo = Silo.builder()
                .companyId(companyId)
                .name(request.name())
                .maxAmount(request.maxAmount())
                .currentAmount(BigDecimal.ZERO)
                .culture(request.culture())
                .comment(request.comment())
                .build();

        return siloMapper.toResponseDto(siloRepository.save(silo));
    }

    @Override
    @Transactional
    public SiloResponse update(UUID id, UpdateSiloRequest request, UUID companyId) {
        Silo silo = findOwned(id, companyId);

        if (request.maxAmount() != null
                && request.maxAmount().compareTo(silo.getCurrentAmount()) < 0) {
            throw WarehouseException.badRequest("Max amount cannot be less than current amount");
        }

        siloMapper.updateFromDto(silo, request);
        return siloMapper.toResponseDto(siloRepository.save(silo));
    }

    @Override
    @Transactional
    public void delete(UUID id, UUID companyId) {
        Silo silo = findOwned(id, companyId);

        if (silo.getCurrentAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw WarehouseException.badRequest("Cannot delete silo with grain inside");
        }

        siloRepository.delete(silo);
    }

    @Override
    @Transactional(readOnly = true)
    public SiloResponse getById(UUID id, UUID companyId) {
        return siloMapper.toResponseDto(findOwned(id, companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SiloResponse> getAll(UUID companyId, SiloFilterRequest filter, Pageable pageable) {
        return siloRepository
                .findAll(SiloSpecification.filter(companyId, filter), pageable)
                .map(siloMapper::toResponseDto);
    }

    @Override
    @Transactional
    public SiloResponse addGrain(UUID id, AddGrainRequest request, UUID companyId) {
        Silo silo = findOwned(id, companyId);

        LabAnalysis labAnalysis = labAnalysisRepository.findById(request.labAnalysisId())
                .orElseThrow(() -> WarehouseException.notFound("Lab analysis not found: " + request.labAnalysisId()));

        if (!labAnalysis.getCompanyId().equals(companyId)) {
            throw WarehouseException.forbidden("Access denied");
        }
        if (labAnalysis.getStatus() != LabStatus.PASSED) {
            throw WarehouseException.badRequest("Lab analysis must be PASSED to add grain to silo");
        }

        BigDecimal amount = labAnalysis.getActualVolume();
        BigDecimal newAmount = silo.getCurrentAmount().add(amount);

        if (newAmount.compareTo(silo.getMaxAmount()) > 0) {
            throw WarehouseException.badRequest(
                    "Not enough capacity. Available: " +
                    silo.getMaxAmount().subtract(silo.getCurrentAmount()) + " tonnes"
            );
        }

        // Set culture from lab analysis if silo is empty
        if (silo.getCulture() == null) {
            silo.setCulture(labAnalysis.getVehicle().getCulture());
        }

        silo.setCurrentAmount(newAmount);

        // Mark lab analysis as STORED
        labAnalysis.setSiloId(id);
        labAnalysis.setStoredAt(LocalDateTime.now());
        labAnalysis.setStatus(LabStatus.STORED);
        labAnalysisRepository.save(labAnalysis);

        return siloMapper.toResponseDto(siloRepository.save(silo));
    }

    @Override
    @Transactional
    public SiloResponse removeGrain(UUID id, RemoveGrainRequest request, UUID companyId) {
        Silo silo = findOwned(id, companyId);

        BigDecimal newAmount = silo.getCurrentAmount().subtract(request.amount());
        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw WarehouseException.badRequest(
                    "Not enough grain. Current amount: " + silo.getCurrentAmount() + " tonnes"
            );
        }

        silo.setCurrentAmount(newAmount);

        // Clear culture when silo is empty
        if (newAmount.compareTo(BigDecimal.ZERO) == 0) {
            silo.setCulture(null);
        }

        return siloMapper.toResponseDto(siloRepository.save(silo));
    }

    private Silo findOwned(UUID id, UUID companyId) {
        Silo silo = siloRepository.findById(id)
                .orElseThrow(() -> WarehouseException.notFound("Silo not found: " + id));
        if (!silo.getCompanyId().equals(companyId)) {
            throw WarehouseException.forbidden("Access denied");
        }
        return silo;
    }
}
