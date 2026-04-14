package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.silo.*;
import com.grainflow.warehouse.entity.*;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.SiloMapper;
import com.grainflow.warehouse.repository.LabAnalysisRepository;
import com.grainflow.warehouse.repository.SiloRepository;
import com.grainflow.warehouse.service.impl.SiloServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SiloServiceImplTest {

    @Mock SiloRepository siloRepository;
    @Mock LabAnalysisRepository labAnalysisRepository;
    @Mock SiloMapper siloMapper;
    @InjectMocks SiloServiceImpl siloService;

    private static final UUID COMPANY_A  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COMPANY_B  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SILO_ID    = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID LAB_ID     = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID VEHICLE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    // ===================== CREATE =====================

    @Test
    void create_success_returnsSiloResponse() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        SiloResponse response = buildResponse(BigDecimal.ZERO, null);

        when(siloRepository.existsByNameAndCompanyId("Silo-A1", COMPANY_A)).thenReturn(false);
        when(siloRepository.save(any())).thenReturn(silo);
        when(siloMapper.toResponseDto(silo)).thenReturn(response);

        SiloResponse result = siloService.create(new CreateSiloRequest("Silo-A1", new BigDecimal("500.000"), null, null), COMPANY_A);

        assertThat(result.currentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(siloRepository).save(any());
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(siloRepository.existsByNameAndCompanyId("Silo-A1", COMPANY_A)).thenReturn(true);

        assertThatThrownBy(() -> siloService.create(new CreateSiloRequest("Silo-A1", new BigDecimal("500.000"), null, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void create_sameNameDifferentCompany_succeeds() {
        when(siloRepository.existsByNameAndCompanyId("Silo-A1", COMPANY_B)).thenReturn(false);
        Silo silo = buildSilo(COMPANY_B, BigDecimal.ZERO, null);
        when(siloRepository.save(any())).thenReturn(silo);
        when(siloMapper.toResponseDto(silo)).thenReturn(buildResponse(BigDecimal.ZERO, null));

        assertThatCode(() -> siloService.create(new CreateSiloRequest("Silo-A1", new BigDecimal("500.000"), null, null), COMPANY_B))
                .doesNotThrowAnyException();
    }

    // ===================== UPDATE =====================

    @Test
    void update_success_returnsUpdatedResponse() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("100.000"), CultureType.WHEAT);
        UpdateSiloRequest request = new UpdateSiloRequest(null, new BigDecimal("600.000"), null, null, null);
        SiloResponse response = buildResponse(new BigDecimal("100.000"), CultureType.WHEAT);

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(siloRepository.save(silo)).thenReturn(silo);
        when(siloMapper.toResponseDto(silo)).thenReturn(response);

        siloService.update(SILO_ID, request, COMPANY_A);

        verify(siloMapper).updateFromDto(silo, request);
    }

    @Test
    void update_maxAmountLessThanCurrent_throwsBadRequest() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("200.000"), CultureType.WHEAT);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        UpdateSiloRequest request = new UpdateSiloRequest(null, new BigDecimal("100.000"), null, null, null);

        assertThatThrownBy(() -> siloService.update(SILO_ID, request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_otherCompany_throwsForbidden() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        assertThatThrownBy(() -> siloService.update(SILO_ID, new UpdateSiloRequest(null, null, null, null, null), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== DELETE =====================

    @Test
    void delete_emptySilo_success() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        siloService.delete(SILO_ID, COMPANY_A);

        verify(siloRepository).delete(silo);
    }

    @Test
    void delete_siloWithGrain_throwsBadRequest() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("100.000"), CultureType.WHEAT);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        assertThatThrownBy(() -> siloService.delete(SILO_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void delete_otherCompany_throwsForbidden() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        assertThatThrownBy(() -> siloService.delete(SILO_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void delete_notFound_throwsNotFound() {
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> siloService.delete(SILO_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ===================== GET BY ID =====================

    @Test
    void getById_success_returnsMappedResponse() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        SiloResponse response = buildResponse(BigDecimal.ZERO, null);

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(siloMapper.toResponseDto(silo)).thenReturn(response);

        assertThat(siloService.getById(SILO_ID, COMPANY_A)).isEqualTo(response);
    }

    @Test
    void getById_otherCompany_throwsForbidden() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        assertThatThrownBy(() -> siloService.getById(SILO_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== GET ALL =====================

    @Test
    void getAll_returnsPage() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        SiloResponse response = buildResponse(BigDecimal.ZERO, null);

        when(siloRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(silo)));
        when(siloMapper.toResponseDto(silo)).thenReturn(response);

        Page<SiloResponse> result = siloService.getAll(COMPANY_A, new SiloFilterRequest(null, null), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // ===================== ADD GRAIN =====================

    @Test
    void addGrain_passedLab_addsVolumeAndSetsStored() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PASSED, new BigDecimal("24.800"));

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(siloRepository.save(silo)).thenReturn(silo);
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(siloMapper.toResponseDto(silo)).thenReturn(buildResponse(new BigDecimal("24.800"), CultureType.WHEAT));

        siloService.addGrain(SILO_ID, new AddGrainRequest(LAB_ID), COMPANY_A);

        assertThat(silo.getCurrentAmount()).isEqualByComparingTo("24.800");
        assertThat(lab.getStatus()).isEqualTo(LabStatus.STORED);
        assertThat(lab.getSiloId()).isEqualTo(SILO_ID);
        assertThat(lab.getStoredAt()).isNotNull();
    }

    @Test
    void addGrain_emptySilo_setsCultureFromLab() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PASSED, new BigDecimal("24.800"));

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(siloRepository.save(silo)).thenReturn(silo);
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(siloMapper.toResponseDto(silo)).thenReturn(buildResponse(new BigDecimal("24.800"), CultureType.WHEAT));

        siloService.addGrain(SILO_ID, new AddGrainRequest(LAB_ID), COMPANY_A);

        assertThat(silo.getCulture()).isEqualTo(CultureType.WHEAT);
    }

    @Test
    void addGrain_siloAlreadyHasGrain_doesNotOverwriteCulture() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("100.000"), CultureType.WHEAT);
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PASSED, new BigDecimal("24.800"));

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(siloRepository.save(silo)).thenReturn(silo);
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(siloMapper.toResponseDto(silo)).thenReturn(buildResponse(new BigDecimal("124.800"), CultureType.WHEAT));

        siloService.addGrain(SILO_ID, new AddGrainRequest(LAB_ID), COMPANY_A);

        assertThat(silo.getCulture()).isEqualTo(CultureType.WHEAT);
        assertThat(silo.getCurrentAmount()).isEqualByComparingTo("124.800");
    }

    @Test
    void addGrain_labNotPassed_throwsBadRequest() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, null);

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> siloService.addGrain(SILO_ID, new AddGrainRequest(LAB_ID), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addGrain_notEnoughCapacity_throwsBadRequest() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("490.000"), CultureType.WHEAT);
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PASSED, new BigDecimal("24.800"));

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> siloService.addGrain(SILO_ID, new AddGrainRequest(LAB_ID), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addGrain_labFromOtherCompany_throwsForbidden() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        LabAnalysis lab = buildLab(COMPANY_B, LabStatus.PASSED, new BigDecimal("24.800"));

        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> siloService.addGrain(SILO_ID, new AddGrainRequest(LAB_ID), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void addGrain_labNotFound_throwsNotFound() {
        Silo silo = buildSilo(COMPANY_A, BigDecimal.ZERO, null);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> siloService.addGrain(SILO_ID, new AddGrainRequest(LAB_ID), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ===================== REMOVE GRAIN =====================

    @Test
    void removeGrain_success_subtractsAmount() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("100.000"), CultureType.WHEAT);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(siloRepository.save(silo)).thenReturn(silo);
        when(siloMapper.toResponseDto(silo)).thenReturn(buildResponse(new BigDecimal("50.000"), CultureType.WHEAT));

        siloService.removeGrain(SILO_ID, new RemoveGrainRequest(new BigDecimal("50.000")), COMPANY_A);

        assertThat(silo.getCurrentAmount()).isEqualByComparingTo("50.000");
        assertThat(silo.getCulture()).isEqualTo(CultureType.WHEAT);
    }

    @Test
    void removeGrain_emptiesSilo_clearsCulture() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("100.000"), CultureType.WHEAT);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));
        when(siloRepository.save(silo)).thenReturn(silo);
        when(siloMapper.toResponseDto(silo)).thenReturn(buildResponse(BigDecimal.ZERO, null));

        siloService.removeGrain(SILO_ID, new RemoveGrainRequest(new BigDecimal("100.000")), COMPANY_A);

        assertThat(silo.getCurrentAmount()).isEqualByComparingTo("0");
        assertThat(silo.getCulture()).isNull();
    }

    @Test
    void removeGrain_notEnoughGrain_throwsBadRequest() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("50.000"), CultureType.WHEAT);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        assertThatThrownBy(() -> siloService.removeGrain(SILO_ID, new RemoveGrainRequest(new BigDecimal("100.000")), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void removeGrain_otherCompany_throwsForbidden() {
        Silo silo = buildSilo(COMPANY_A, new BigDecimal("100.000"), CultureType.WHEAT);
        when(siloRepository.findById(SILO_ID)).thenReturn(Optional.of(silo));

        assertThatThrownBy(() -> siloService.removeGrain(SILO_ID, new RemoveGrainRequest(new BigDecimal("50.000")), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== HELPERS =====================

    private Silo buildSilo(UUID companyId, BigDecimal currentAmount, CultureType culture) {
        Silo silo = new Silo();
        silo.setId(SILO_ID);
        silo.setCompanyId(companyId);
        silo.setName("Silo-A1");
        silo.setMaxAmount(new BigDecimal("500.000"));
        silo.setCurrentAmount(currentAmount);
        silo.setCulture(culture);
        silo.setCreatedAt(LocalDateTime.now());
        silo.setUpdatedAt(LocalDateTime.now());
        return silo;
    }

    private LabAnalysis buildLab(UUID companyId, LabStatus status, BigDecimal actualVolume) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setCompanyId(companyId);
        vehicle.setCulture(CultureType.WHEAT);
        vehicle.setDeclaredVolume(new BigDecimal("25.500"));
        vehicle.setStatus(VehicleStatus.ACCEPTED);
        vehicle.setArrivedAt(LocalDateTime.now());

        Batch batch = new Batch();
        batch.setId(UUID.randomUUID());
        batch.setCompanyId(companyId);
        batch.setStatus(BatchStatus.ACTIVE);
        batch.setContractNumber("CONTRACT-001");
        batch.setCulture(CultureType.WHEAT);
        batch.setTotalVolume(new BigDecimal("500.000"));
        batch.setAcceptedVolume(BigDecimal.ZERO);
        batch.setUnloadedVolume(BigDecimal.ZERO);
        batch.setLoadingFrom(LocalDate.of(2026, 1, 1));
        batch.setLoadingTo(LocalDate.of(2026, 12, 31));
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());
        vehicle.setBatch(batch);

        LabAnalysis lab = new LabAnalysis();
        lab.setId(LAB_ID);
        lab.setCompanyId(companyId);
        lab.setVehicle(vehicle);
        lab.setStatus(status);
        lab.setActualVolume(actualVolume);
        lab.setCreatedAt(LocalDateTime.now());
        lab.setUpdatedAt(LocalDateTime.now());
        return lab;
    }

    private SiloResponse buildResponse(BigDecimal currentAmount, CultureType culture) {
        return new SiloResponse(SILO_ID, COMPANY_A, "Silo-A1",
                new BigDecimal("500.000"), currentAmount, culture, null,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
