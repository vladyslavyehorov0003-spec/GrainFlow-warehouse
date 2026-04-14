package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.lab.*;
import com.grainflow.warehouse.entity.*;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.LabAnalysisMapper;
import com.grainflow.warehouse.repository.LabAnalysisRepository;
import com.grainflow.warehouse.repository.VehicleRepository;
import com.grainflow.warehouse.service.impl.LabAnalysisServiceImpl;
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
class LabAnalysisServiceImplTest {

    @Mock LabAnalysisRepository labAnalysisRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock LabAnalysisMapper labAnalysisMapper;
    @InjectMocks LabAnalysisServiceImpl labAnalysisService;

    private static final UUID COMPANY_A  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COMPANY_B  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VEHICLE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID LAB_ID     = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    // ===================== CREATE =====================

    @Test
    void create_success_returnsPendingResponse() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        LabAnalysisResponse response = buildResponse(LabStatus.PENDING);

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(labAnalysisRepository.existsByVehicleId(VEHICLE_ID)).thenReturn(false);
        when(labAnalysisRepository.save(any())).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(response);

        LabAnalysisResponse result = labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A);

        assertThat(result.status()).isEqualTo(LabStatus.PENDING);
    }

    @Test
    void create_vehicleNotFound_throwsNotFound() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_vehicleOtherCompany_throwsForbidden() {
        Vehicle vehicle = buildVehicle(COMPANY_B, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void create_rejectedVehicle_throwsBadRequest() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.REJECTED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_duplicateAnalysis_throwsConflict() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(labAnalysisRepository.existsByVehicleId(VEHICLE_ID)).thenReturn(true);

        assertThatThrownBy(() -> labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    // ===================== UPDATE =====================

    @Test
    void update_pendingLab_success() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        UpdateLabAnalysisRequest request = new UpdateLabAnalysisRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, "updated comment");

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.PENDING));

        labAnalysisService.update(LAB_ID, request, COMPANY_A);

        verify(labAnalysisMapper).updateFromDto(lab, request);
    }

    @Test
    void update_passedLab_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PASSED);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.update(LAB_ID, anyUpdateRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_failedLab_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.FAILED);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.update(LAB_ID, anyUpdateRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.update(LAB_ID, anyUpdateRequest(), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== GET BY ID =====================

    @Test
    void getById_success_returnsMappedResponse() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        LabAnalysisResponse response = buildResponse(LabStatus.PENDING);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(response);

        assertThat(labAnalysisService.getById(LAB_ID, COMPANY_A)).isEqualTo(response);
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labAnalysisService.getById(LAB_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getById_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.getById(LAB_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== GET ALL =====================

    @Test
    void getAll_returnsPage() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        LabAnalysisResponse response = buildResponse(LabStatus.PENDING);

        when(labAnalysisRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(lab)));
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(response);

        Page<LabAnalysisResponse> result = labAnalysisService.getAll(COMPANY_A, emptyFilter(), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // ===================== START =====================

    @Test
    void start_pendingLab_setsStatusAndTimestamp() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.IN_PROGRESS));

        labAnalysisService.start(LAB_ID, COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.IN_PROGRESS);
        assertThat(lab.getAnalysisStartedAt()).isNotNull();
    }

    @Test
    void start_notPending_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.start(LAB_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===================== START DRYING =====================

    @Test
    void startDrying_inProgressLab_setsVolumeAndTimestamp() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        StartDryingRequest request = new StartDryingRequest(new BigDecimal("25.500"), null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.IN_PROGRESS));

        labAnalysisService.startDrying(LAB_ID, request, COMPANY_A);

        assertThat(lab.getVolumeBeforeDrying()).isEqualByComparingTo("25.500");
        assertThat(lab.getDryingStartedAt()).isNotNull();
    }

    @Test
    void startDrying_withEstimatedEnd_savesIt() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        LocalDateTime estimated = LocalDateTime.of(2026, 4, 14, 14, 0);
        StartDryingRequest request = new StartDryingRequest(new BigDecimal("25.500"), estimated);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.IN_PROGRESS));

        labAnalysisService.startDrying(LAB_ID, request, COMPANY_A);

        assertThat(lab.getEstimatedDryingEndAt()).isEqualTo(estimated);
    }

    @Test
    void startDrying_notInProgress_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.startDrying(LAB_ID, new StartDryingRequest(new BigDecimal("25.500"), null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void startDrying_alreadyStarted_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        lab.setDryingStartedAt(LocalDateTime.now());
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.startDrying(LAB_ID, new StartDryingRequest(new BigDecimal("25.500"), null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===================== FINISH DRYING =====================

    @Test
    void finishDrying_success_setsVolumeAndTimestamp() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        lab.setDryingStartedAt(LocalDateTime.now().minusHours(2));
        FinishDryingRequest request = new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00"));

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.IN_PROGRESS));

        labAnalysisService.finishDrying(LAB_ID, request, COMPANY_A);

        assertThat(lab.getVolumeAfterDrying()).isEqualByComparingTo("24.800");
        assertThat(lab.getMoistureAfterDrying()).isEqualByComparingTo("13.00");
        assertThat(lab.getDryingFinishedAt()).isNotNull();
    }

    @Test
    void finishDrying_dryingNotStarted_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishDrying(LAB_ID, new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00")), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void finishDrying_alreadyFinished_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        lab.setDryingStartedAt(LocalDateTime.now().minusHours(2));
        lab.setDryingFinishedAt(LocalDateTime.now().minusHours(1));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishDrying(LAB_ID, new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00")), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===================== FINISH ANALYSIS =====================

    @Test
    void finishAnalysis_passed_setsAllFields() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        FinishAnalysisRequest request = new FinishAnalysisRequest(
                new BigDecimal("14.50"), new BigDecimal("1.20"), new BigDecimal("12.00"),
                new BigDecimal("24.800"), LabStatus.PASSED, null
        );

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.PASSED));

        labAnalysisService.finishAnalysis(LAB_ID, request, COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.PASSED);
        assertThat(lab.getMoisture()).isEqualByComparingTo("14.50");
        assertThat(lab.getImpurity()).isEqualByComparingTo("1.20");
        assertThat(lab.getActualVolume()).isEqualByComparingTo("24.800");
        assertThat(lab.getAnalysisFinishedAt()).isNotNull();
        assertThat(lab.getDecidedAt()).isNotNull();
    }

    @Test
    void finishAnalysis_failed_withComment_setsComment() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        FinishAnalysisRequest request = new FinishAnalysisRequest(
                new BigDecimal("28.50"), new BigDecimal("5.00"), null,
                new BigDecimal("25.500"), LabStatus.FAILED, "moisture too high"
        );

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.FAILED));

        labAnalysisService.finishAnalysis(LAB_ID, request, COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.FAILED);
        assertThat(lab.getComment()).isEqualTo("moisture too high");
    }

    @Test
    void finishAnalysis_withoutDrying_succeeds() {
        // Drying is optional — should pass even if dryingStartedAt is null
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        FinishAnalysisRequest request = new FinishAnalysisRequest(
                new BigDecimal("14.50"), new BigDecimal("1.20"), null,
                new BigDecimal("25.500"), LabStatus.PASSED, null
        );

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.PASSED));

        assertThatCode(() -> labAnalysisService.finishAnalysis(LAB_ID, request, COMPANY_A)).doesNotThrowAnyException();
    }

    @Test
    void finishAnalysis_notInProgress_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishAnalysis(LAB_ID,
                new FinishAnalysisRequest(new BigDecimal("14.50"), new BigDecimal("1.20"), null, new BigDecimal("24.800"), LabStatus.PASSED, null),
                COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void finishAnalysis_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishAnalysis(LAB_ID,
                new FinishAnalysisRequest(new BigDecimal("14.50"), new BigDecimal("1.20"), null, new BigDecimal("24.800"), LabStatus.PASSED, null),
                COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== HELPERS =====================

    private Vehicle buildVehicle(UUID companyId, VehicleStatus status) {
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

        Vehicle vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setCompanyId(companyId);
        vehicle.setBatch(batch);
        vehicle.setLicensePlate("AA1234BB");
        vehicle.setCulture(CultureType.WHEAT);
        vehicle.setDeclaredVolume(new BigDecimal("25.500"));
        vehicle.setStatus(status);
        vehicle.setArrivedAt(LocalDateTime.now());
        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());
        return vehicle;
    }

    private LabAnalysis buildLab(UUID companyId, LabStatus status) {
        Vehicle vehicle = buildVehicle(companyId, VehicleStatus.IN_PROCESS);
        LabAnalysis lab = new LabAnalysis();
        lab.setId(LAB_ID);
        lab.setCompanyId(companyId);
        lab.setVehicle(vehicle);
        lab.setStatus(status);
        lab.setCreatedAt(LocalDateTime.now());
        lab.setUpdatedAt(LocalDateTime.now());
        return lab;
    }

    private LabAnalysisResponse buildResponse(LabStatus status) {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A)
                .vehicleId(VEHICLE_ID)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UpdateLabAnalysisRequest anyUpdateRequest() {
        return new UpdateLabAnalysisRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private LabAnalysisFilterRequest emptyFilter() {
        return new LabAnalysisFilterRequest(null, null, null);
    }
}
