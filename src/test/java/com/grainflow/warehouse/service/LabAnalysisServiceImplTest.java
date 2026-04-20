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
import org.springframework.test.util.ReflectionTestUtils;

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
    @Mock VehicleRepository    vehicleRepository;
    @Mock LabAnalysisMapper    labAnalysisMapper;
    @Mock VehicleService       vehicleService;

    @InjectMocks LabAnalysisServiceImpl labAnalysisService;

    private static final UUID COMPANY_A  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COMPANY_B  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID VEHICLE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID LAB_ID     = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    // ───────────────────────── CREATE ─────────────────────────────────────────

    @Test
    void create_success_returnsPendingResponse() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.ARRIVED);
        LabAnalysisResponse response = buildResponse(LabStatus.PENDING, ApprovalStatus.PENDING);

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(labAnalysisRepository.existsByVehicleId(VEHICLE_ID)).thenReturn(false);
        when(labAnalysisRepository.save(any())).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(response);

        LabAnalysisResponse result = labAnalysisService.create(
                new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A);

        assertThat(result.status()).isEqualTo(LabStatus.PENDING);
    }

    @Test
    void create_vehicleNotFound_throwsNotFound() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_vehicleOtherCompany_throwsForbidden() {
        Vehicle vehicle = buildVehicle(COMPANY_B, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() ->
                labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void create_rejectedVehicle_throwsBadRequest() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.REJECTED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() ->
                labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_duplicateAnalysis_throwsConflict() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(labAnalysisRepository.existsByVehicleId(VEHICLE_ID)).thenReturn(true);

        assertThatThrownBy(() ->
                labAnalysisService.create(new CreateLabAnalysisRequest(VEHICLE_ID, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    // ───────────────────────── UPDATE ─────────────────────────────────────────

    @Test
    void update_pendingLab_success() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        UpdateLabAnalysisRequest request = anyUpdateRequest();

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.PENDING, ApprovalStatus.PENDING));

        labAnalysisService.update(LAB_ID, request, COMPANY_A);

        verify(labAnalysisMapper).updateFromDto(lab, request);
    }

    @Test
    void update_inProgressLab_success() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.IN_PROCESS);
        UpdateLabAnalysisRequest request = anyUpdateRequest();

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.IN_PROGRESS, ApprovalStatus.PENDING));

        assertThatCode(() -> labAnalysisService.update(LAB_ID, request, COMPANY_A)).doesNotThrowAnyException();
    }

    @Test
    void update_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.update(LAB_ID, anyUpdateRequest(), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void update_notFound_throwsNotFound() {
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labAnalysisService.update(LAB_ID, anyUpdateRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ───────────────────────── GET BY ID ──────────────────────────────────────

    @Test
    void getById_success_returnsMappedResponse() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        LabAnalysisResponse response = buildResponse(LabStatus.PENDING, ApprovalStatus.PENDING);

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
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.getById(LAB_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ───────────────────────── GET ALL ────────────────────────────────────────

    @Test
    void getAll_returnsPage() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        LabAnalysisResponse response = buildResponse(LabStatus.PENDING, ApprovalStatus.PENDING);

        when(labAnalysisRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(lab)));
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(response);

        Page<LabAnalysisResponse> result = labAnalysisService.getAll(
                COMPANY_A, emptyFilter(), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // ───────────────────────── START ──────────────────────────────────────────

    @Test
    void start_pendingLab_setsStatusAndTimestamp() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.IN_PROGRESS, ApprovalStatus.PENDING));

        labAnalysisService.start(LAB_ID, COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.IN_PROGRESS);
        assertThat(lab.getAnalysisStartedAt()).isNotNull();
    }

    @Test
    void start_notPending_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.start(LAB_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void start_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.start(LAB_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ───────────────────────── FINISH ANALYSIS ────────────────────────────────

    @Test
    void finishAnalysis_inProgress_setsAnalysisDoneAndFields() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.IN_PROCESS);
        FinishAnalysisRequest request = new FinishAnalysisRequest(
                new BigDecimal("14.50"), new BigDecimal("1.20"),
                new BigDecimal("12.00"), new BigDecimal("24.800"), null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.ANALYSIS_DONE, ApprovalStatus.PENDING));

        labAnalysisService.finishAnalysis(LAB_ID, request, COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.ANALYSIS_DONE);
        assertThat(lab.getMoisture()).isEqualByComparingTo("14.50");
        assertThat(lab.getImpurity()).isEqualByComparingTo("1.20");
        assertThat(lab.getActualVolume()).isEqualByComparingTo("24.800");
        assertThat(lab.getAnalysisFinishedAt()).isNotNull();
    }

    @Test
    void finishAnalysis_withComment_savesComment() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.IN_PROCESS);
        FinishAnalysisRequest request = new FinishAnalysisRequest(
                new BigDecimal("28.50"), new BigDecimal("5.00"), null,
                new BigDecimal("25.500"), "high moisture, needs drying");

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.ANALYSIS_DONE, ApprovalStatus.PENDING));

        labAnalysisService.finishAnalysis(LAB_ID, request, COMPANY_A);

        assertThat(lab.getComment()).isEqualTo("high moisture, needs drying");
    }

    @Test
    void finishAnalysis_withoutProtein_succeeds() {
        // Protein is optional
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.IN_PROCESS);
        FinishAnalysisRequest request = new FinishAnalysisRequest(
                new BigDecimal("14.50"), new BigDecimal("1.20"), null,
                new BigDecimal("25.500"), null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.ANALYSIS_DONE, ApprovalStatus.PENDING));

        assertThatCode(() -> labAnalysisService.finishAnalysis(LAB_ID, request, COMPANY_A))
                .doesNotThrowAnyException();
    }

    @Test
    void finishAnalysis_notInProgress_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishAnalysis(LAB_ID, anyFinishAnalysisRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void finishAnalysis_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishAnalysis(LAB_ID, anyFinishAnalysisRequest(), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ───────────────────────── START DRYING ───────────────────────────────────

    @Test
    void startDrying_analysisDone_acceptedVehicle_setsVolumeAndTimestamp() {
        LabAnalysis lab = buildLabWithActualVolume(
                COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.ACCEPTED, new BigDecimal("25.500"));
        StartDryingRequest request = new StartDryingRequest(new BigDecimal("25.000"), null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.DRYING, ApprovalStatus.PENDING));

        labAnalysisService.startDrying(LAB_ID, request, COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.DRYING);
        assertThat(lab.getVolumeBeforeDrying()).isEqualByComparingTo("25.000");
        assertThat(lab.getDryingStartedAt()).isNotNull();
    }

    @Test
    void startDrying_withEstimatedEnd_savesIt() {
        LabAnalysis lab = buildLabWithActualVolume(
                COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.ACCEPTED, new BigDecimal("25.500"));
        LocalDateTime estimated = LocalDateTime.of(2026, 4, 14, 14, 0);
        StartDryingRequest request = new StartDryingRequest(new BigDecimal("25.000"), estimated);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.DRYING, ApprovalStatus.PENDING));

        labAnalysisService.startDrying(LAB_ID, request, COMPANY_A);

        assertThat(lab.getEstimatedDryingEndAt()).isEqualTo(estimated);
    }

    @Test
    void startDrying_volumeExceedsActual_throwsBadRequest() {
        // volumeBeforeDrying > actualVolume → BAD_REQUEST (validated first in service)
        LabAnalysis lab = buildLabWithActualVolume(
                COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.ACCEPTED, new BigDecimal("20.000"));
        StartDryingRequest request = new StartDryingRequest(new BigDecimal("25.000"), null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.startDrying(LAB_ID, request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void startDrying_notAnalysisDone_throwsBadRequest() {
        LabAnalysis lab = buildLabWithActualVolume(
                COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.ACCEPTED, new BigDecimal("25.500"));
        StartDryingRequest request = new StartDryingRequest(new BigDecimal("25.000"), null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.startDrying(LAB_ID, request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void startDrying_vehicleNotAccepted_throwsBadRequest() {
        LabAnalysis lab = buildLabWithActualVolume(
                COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.PENDING_REVIEW, new BigDecimal("25.500"));
        StartDryingRequest request = new StartDryingRequest(new BigDecimal("25.000"), null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.startDrying(LAB_ID, request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void startDrying_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLabWithActualVolume(
                COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.ACCEPTED, new BigDecimal("25.500"));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() ->
                labAnalysisService.startDrying(LAB_ID, new StartDryingRequest(new BigDecimal("25.000"), null), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ───────────────────────── FINISH DRYING ──────────────────────────────────

    @Test
    void finishDrying_drying_setsVolumeAndTimestamp() {
        LabAnalysis lab = buildLabWithDryingStarted(COMPANY_A, new BigDecimal("25.000"));
        FinishDryingRequest request = new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00"));

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.DRYING_DONE, ApprovalStatus.PENDING));

        labAnalysisService.finishDrying(LAB_ID, request, COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.DRYING_DONE);
        assertThat(lab.getVolumeAfterDrying()).isEqualByComparingTo("24.800");
        assertThat(lab.getMoistureAfterDrying()).isEqualByComparingTo("13.00");
        assertThat(lab.getDryingFinishedAt()).isNotNull();
    }

    @Test
    void finishDrying_volumeAfterExceedsBefore_throwsBadRequest() {
        LabAnalysis lab = buildLabWithDryingStarted(COMPANY_A, new BigDecimal("20.000"));
        FinishDryingRequest request = new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00"));

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishDrying(LAB_ID, request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void finishDrying_notDrying_throwsBadRequest() {
        // status is ANALYSIS_DONE, not DRYING
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.ACCEPTED);
        lab.setVolumeBeforeDrying(new BigDecimal("25.000"));
        FinishDryingRequest request = new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00"));

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.finishDrying(LAB_ID, request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void finishDrying_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLabWithDryingStarted(COMPANY_A, new BigDecimal("25.000"));
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() ->
                labAnalysisService.finishDrying(LAB_ID, new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00")), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ───────────────────────── RELEASE ────────────────────────────────────────

    @Test
    void release_analysisDone_approvedTrue_setsApprovedAndCallsFinishProcessing() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.IN_PROCESS);
        ReleaseLabRequest request = new ReleaseLabRequest(true, null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.ANALYSIS_DONE, ApprovalStatus.APPROVED));

        // inject vehicleService since @Lazy field won't be set by @InjectMocks
        ReflectionTestUtils.setField(labAnalysisService, "vehicleService", vehicleService);

        labAnalysisService.release(LAB_ID, request, COMPANY_A);

        assertThat(lab.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(lab.getDecidedAt()).isNotNull();
        verify(vehicleService).finishProcessing(VEHICLE_ID, COMPANY_A);
    }

    @Test
    void release_approvedFalse_setsRejected() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.IN_PROCESS);
        ReleaseLabRequest request = new ReleaseLabRequest(false, "grain is contaminated");

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.ANALYSIS_DONE, ApprovalStatus.REJECTED));

        ReflectionTestUtils.setField(labAnalysisService, "vehicleService", vehicleService);

        labAnalysisService.release(LAB_ID, request, COMPANY_A);

        assertThat(lab.getApprovalStatus()).isEqualTo(ApprovalStatus.REJECTED);
        assertThat(lab.getComment()).isEqualTo("grain is contaminated");
    }

    @Test
    void release_fromDryingStatus_succeeds() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.DRYING, VehicleStatus.ACCEPTED);
        ReleaseLabRequest request = new ReleaseLabRequest(true, null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.DRYING, ApprovalStatus.APPROVED));

        ReflectionTestUtils.setField(labAnalysisService, "vehicleService", vehicleService);

        assertThatCode(() -> labAnalysisService.release(LAB_ID, request, COMPANY_A))
                .doesNotThrowAnyException();
    }

    @Test
    void release_fromDryingDoneStatus_succeeds() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.DRYING_DONE, VehicleStatus.ACCEPTED);
        ReleaseLabRequest request = new ReleaseLabRequest(true, null);

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.DRYING_DONE, ApprovalStatus.APPROVED));

        ReflectionTestUtils.setField(labAnalysisService, "vehicleService", vehicleService);

        assertThatCode(() -> labAnalysisService.release(LAB_ID, request, COMPANY_A))
                .doesNotThrowAnyException();
    }

    @Test
    void release_fromInProgress_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.IN_PROGRESS, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.release(LAB_ID, new ReleaseLabRequest(true, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void release_fromPending_throwsBadRequest() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.PENDING, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.release(LAB_ID, new ReleaseLabRequest(true, null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void release_otherCompany_throwsForbidden() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.IN_PROCESS);
        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));

        assertThatThrownBy(() -> labAnalysisService.release(LAB_ID, new ReleaseLabRequest(true, null), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void release_withComment_savesComment() {
        LabAnalysis lab = buildLab(COMPANY_A, LabStatus.ANALYSIS_DONE, VehicleStatus.IN_PROCESS);
        ReleaseLabRequest request = new ReleaseLabRequest(true, "looks good");

        when(labAnalysisRepository.findById(LAB_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);
        when(labAnalysisMapper.toResponseDto(lab)).thenReturn(buildResponse(LabStatus.ANALYSIS_DONE, ApprovalStatus.APPROVED));

        ReflectionTestUtils.setField(labAnalysisService, "vehicleService", vehicleService);

        labAnalysisService.release(LAB_ID, request, COMPANY_A);

        assertThat(lab.getComment()).isEqualTo("looks good");
    }

    // ───────────────────────── HELPERS ────────────────────────────────────────

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

    private LabAnalysis buildLab(UUID companyId, LabStatus status, VehicleStatus vehicleStatus) {
        Vehicle vehicle = buildVehicle(companyId, vehicleStatus);
        LabAnalysis lab = new LabAnalysis();
        lab.setId(LAB_ID);
        lab.setCompanyId(companyId);
        lab.setVehicle(vehicle);
        lab.setStatus(status);
        lab.setApprovalStatus(ApprovalStatus.PENDING);
        lab.setCreatedAt(LocalDateTime.now());
        lab.setUpdatedAt(LocalDateTime.now());
        return lab;
    }

    /** Lab in ANALYSIS_DONE status with actualVolume set, needed for startDrying volume check. */
    private LabAnalysis buildLabWithActualVolume(UUID companyId, LabStatus status,
                                                  VehicleStatus vehicleStatus,
                                                  BigDecimal actualVolume) {
        LabAnalysis lab = buildLab(companyId, status, vehicleStatus);
        lab.setActualVolume(actualVolume);
        return lab;
    }

    /** Lab already in DRYING with dryingStartedAt and volumeBeforeDrying set. */
    private LabAnalysis buildLabWithDryingStarted(UUID companyId, BigDecimal volumeBeforeDrying) {
        LabAnalysis lab = buildLab(companyId, LabStatus.DRYING, VehicleStatus.ACCEPTED);
        lab.setActualVolume(volumeBeforeDrying.add(new BigDecimal("1.000")));
        lab.setVolumeBeforeDrying(volumeBeforeDrying);
        lab.setDryingStartedAt(LocalDateTime.now().minusHours(2));
        return lab;
    }

    private LabAnalysisResponse buildResponse(LabStatus status, ApprovalStatus approvalStatus) {
        return LabAnalysisResponse.builder()
                .id(LAB_ID)
                .companyId(COMPANY_A)
                .vehicleId(VEHICLE_ID)
                .status(status)
                .approvalStatus(approvalStatus)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private UpdateLabAnalysisRequest anyUpdateRequest() {
        // moisture, impurity, protein, volumeBeforeDrying, volumeAfterDrying, moistureAfterDrying,
        // actualVolume, analysisStartedAt, analysisFinishedAt, dryingStartedAt, dryingFinishedAt,
        // decidedAt, approved (boolean), status, comment
        return new UpdateLabAnalysisRequest(null, null, null, null, null, null, null, null, null, null, null, null, false, null, null);
    }

    private FinishAnalysisRequest anyFinishAnalysisRequest() {
        return new FinishAnalysisRequest(
                new BigDecimal("14.50"), new BigDecimal("1.20"), null,
                new BigDecimal("24.800"), null);
    }

    private LabAnalysisFilterRequest emptyFilter() {
        return new LabAnalysisFilterRequest(null, null, null);
    }
}
