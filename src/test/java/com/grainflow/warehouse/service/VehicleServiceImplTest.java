package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.lab.CreateLabAnalysisRequest;
import com.grainflow.warehouse.dto.vehicle.CreateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.UpdateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleFilterRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleResponse;
import com.grainflow.warehouse.entity.*;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.VehicleMapper;
import com.grainflow.warehouse.dto.lab.CreateLabAnalysisRequest;
import com.grainflow.warehouse.repository.BatchRepository;
import com.grainflow.warehouse.repository.LabAnalysisRepository;
import com.grainflow.warehouse.repository.VehicleRepository;
import com.grainflow.warehouse.service.LabAnalysisService;
import com.grainflow.warehouse.service.impl.VehicleServiceImpl;
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
class VehicleServiceImplTest {

    @Mock VehicleRepository      vehicleRepository;
    @Mock BatchRepository        batchRepository;
    @Mock VehicleMapper          vehicleMapper;
    @Mock LabAnalysisService     labAnalysisService;
    @Mock LabAnalysisRepository  labAnalysisRepository;
    @InjectMocks VehicleServiceImpl vehicleService;

    private static final UUID COMPANY_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COMPANY_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BATCH_ID  = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID VEHICLE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    // ===================== CREATE =====================

    @Test
    void create_success_returnsVehicleResponse() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE);
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        VehicleResponse response = buildResponse(VehicleStatus.ARRIVED);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(vehicleRepository.save(any())).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(response);

        VehicleResponse result = vehicleService.create(validCreateRequest(null), COMPANY_A);

        assertThat(result.status()).isEqualTo(VehicleStatus.ARRIVED);
    }

    @Test
    void create_customArrivedAt_usesProvidedTime() {
        LocalDateTime customTime = LocalDateTime.of(2026, 4, 14, 7, 30);
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE);
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        vehicle.setArrivedAt(customTime);
        VehicleResponse response = buildResponse(VehicleStatus.ARRIVED);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(vehicleRepository.save(any())).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(response);

        vehicleService.create(validCreateRequest(customTime), COMPANY_A);

        verify(vehicleRepository).save(argThat(v -> v.getArrivedAt().equals(customTime)));
    }

    @Test
    void create_batchNotFound_throwsNotFound() {
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.create(validCreateRequest(null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_batchBelongsToOtherCompany_throwsForbidden() {
        Batch batch = buildBatch(COMPANY_B, BatchStatus.ACTIVE);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> vehicleService.create(validCreateRequest(null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void create_closedBatch_throwsBadRequest() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.CLOSED);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> vehicleService.create(validCreateRequest(null), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===================== UPDATE =====================

    @Test
    void update_arrivedVehicle_success() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        VehicleResponse response = buildResponse(VehicleStatus.ARRIVED);
        UpdateVehicleRequest request = new UpdateVehicleRequest(null, "New Driver", null, null, null, null, null, null, null);

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(response);

        vehicleService.update(VEHICLE_ID, request, COMPANY_A);

        verify(vehicleMapper).updateVehicleFromDto(vehicle, request);
    }

    @Test
    void update_inProcessVehicle_success() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.IN_PROCESS);
        VehicleResponse response = buildResponse(VehicleStatus.IN_PROCESS);
        UpdateVehicleRequest request = new UpdateVehicleRequest(null, null, null, null, null, null, null, null, "note");

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(response);

        assertThatCode(() -> vehicleService.update(VEHICLE_ID, request, COMPANY_A)).doesNotThrowAnyException();
    }

    @Test
    void update_acceptedVehicle_throwsBadRequest() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ACCEPTED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.update(VEHICLE_ID, anyUpdateRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_rejectedVehicle_throwsBadRequest() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.REJECTED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.update(VEHICLE_ID, anyUpdateRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_otherCompany_throwsForbidden() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.update(VEHICLE_ID, anyUpdateRequest(), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== GET BY ID =====================

    @Test
    void getById_success_returnsMappedResponse() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        VehicleResponse response = buildResponse(VehicleStatus.ARRIVED);

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(response);

        assertThat(vehicleService.getById(VEHICLE_ID, COMPANY_A)).isEqualTo(response);
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getById(VEHICLE_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getById_otherCompany_throwsForbidden() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.getById(VEHICLE_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== GET ALL =====================

    @Test
    void getAll_returnsPage() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        VehicleResponse response = buildResponse(VehicleStatus.ARRIVED);

        when(vehicleRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(response);

        Page<VehicleResponse> result = vehicleService.getAll(COMPANY_A, emptyFilter(), PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
    }

    // ===================== START PROCESSING =====================

    @Test
    void startProcessing_arrivedVehicle_setsStatusAndTimestamp() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(buildResponse(VehicleStatus.IN_PROCESS));
        // auto-creates lab analysis in the same transaction
        when(labAnalysisService.create(any(CreateLabAnalysisRequest.class), eq(COMPANY_A)))
                .thenReturn(null);

        vehicleService.startProcessing(VEHICLE_ID, COMPANY_A);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_PROCESS);
        assertThat(vehicle.getUnloadingStartedAt()).isNotNull();
        verify(labAnalysisService).create(any(CreateLabAnalysisRequest.class), eq(COMPANY_A));
    }

    @Test
    void startProcessing_notArrived_throwsBadRequest() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.IN_PROCESS);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.startProcessing(VEHICLE_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===================== FINISH PROCESSING =====================

    @Test
    void finishProcessing_inProcessVehicle_setsPendingReviewAndTimestamp() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.IN_PROCESS);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(buildResponse(VehicleStatus.PENDING_REVIEW));

        vehicleService.finishProcessing(VEHICLE_ID, COMPANY_A);

        assertThat(vehicle.getUnloadingFinishedAt()).isNotNull();
        // Status moves to PENDING_REVIEW — lab released vehicle for manager decision
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.PENDING_REVIEW);
    }

    @Test
    void finishProcessing_notInProcess_throwsBadRequest() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.finishProcessing(VEHICLE_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===================== ACCEPT =====================

    @Test
    void accept_pendingReviewVehicle_setsStatusAndDecidedAt() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.PENDING_REVIEW);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(buildResponse(VehicleStatus.ACCEPTED));

        vehicleService.accept(VEHICLE_ID, COMPANY_A);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.ACCEPTED);
        assertThat(vehicle.getDecidedAt()).isNotNull();
    }

    @Test
    void accept_arrivedVehicle_throwsBadRequest() {
        // ARRIVED is not PENDING_REVIEW → BAD_REQUEST
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.accept(VEHICLE_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void accept_inProcessVehicle_throwsBadRequest() {
        // IN_PROCESS is not PENDING_REVIEW → BAD_REQUEST
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.IN_PROCESS);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.accept(VEHICLE_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void accept_otherCompany_throwsForbidden() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.PENDING_REVIEW);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.accept(VEHICLE_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== REJECT =====================

    @Test
    void reject_pendingReviewVehicle_withComment_setsStatusAndComment() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.PENDING_REVIEW);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(buildResponse(VehicleStatus.REJECTED));
        when(labAnalysisRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());

        vehicleService.reject(VEHICLE_ID, "moisture too high", COMPANY_A);

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.REJECTED);
        assertThat(vehicle.getDecidedAt()).isNotNull();
        assertThat(vehicle.getComment()).isEqualTo("moisture too high");
    }

    @Test
    void reject_pendingReviewVehicle_withoutComment_doesNotOverwriteComment() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.PENDING_REVIEW);
        vehicle.setComment("original comment");
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(buildResponse(VehicleStatus.REJECTED));
        when(labAnalysisRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.empty());

        vehicleService.reject(VEHICLE_ID, null, COMPANY_A);

        // null comment — original should remain
        assertThat(vehicle.getComment()).isEqualTo("original comment");
    }

    @Test
    void reject_cancelsLabAnalysisIfPresent() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.PENDING_REVIEW);
        LabAnalysis lab = new LabAnalysis();
        lab.setId(UUID.randomUUID());
        lab.setStatus(LabStatus.ANALYSIS_DONE);

        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(vehicle)).thenReturn(vehicle);
        when(vehicleMapper.toResponseDto(vehicle)).thenReturn(buildResponse(VehicleStatus.REJECTED));
        when(labAnalysisRepository.findByVehicleId(VEHICLE_ID)).thenReturn(Optional.of(lab));
        when(labAnalysisRepository.save(lab)).thenReturn(lab);

        vehicleService.reject(VEHICLE_ID, "rejected", COMPANY_A);

        assertThat(lab.getStatus()).isEqualTo(LabStatus.CANCELED);
        verify(labAnalysisRepository).save(lab);
    }

    @Test
    void reject_arrivedVehicle_throwsBadRequest() {
        // ARRIVED is not PENDING_REVIEW → BAD_REQUEST
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.ARRIVED);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.reject(VEHICLE_ID, null, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void reject_otherCompany_throwsForbidden() {
        Vehicle vehicle = buildVehicle(COMPANY_A, VehicleStatus.PENDING_REVIEW);
        when(vehicleRepository.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> vehicleService.reject(VEHICLE_ID, null, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== HELPERS =====================

    private Batch buildBatch(UUID companyId, BatchStatus status) {
        Batch batch = new Batch();
        batch.setId(BATCH_ID);
        batch.setCompanyId(companyId);
        batch.setStatus(status);
        batch.setContractNumber("CONTRACT-001");
        batch.setCulture(CultureType.WHEAT);
        batch.setTotalVolume(new BigDecimal("500.000"));
        batch.setAcceptedVolume(BigDecimal.ZERO);
        batch.setUnloadedVolume(BigDecimal.ZERO);
        batch.setLoadingFrom(LocalDate.of(2026, 1, 1));
        batch.setLoadingTo(LocalDate.of(2026, 12, 31));
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());
        return batch;
    }

    private Vehicle buildVehicle(UUID companyId, VehicleStatus status) {
        Batch batch = buildBatch(companyId, BatchStatus.ACTIVE);
        Vehicle vehicle = new Vehicle();
        vehicle.setId(VEHICLE_ID);
        vehicle.setCompanyId(companyId);
        vehicle.setBatch(batch);
        vehicle.setLicensePlate("AA1234BB");
        vehicle.setDriverName("Ivan Petrenko");
        vehicle.setCulture(CultureType.WHEAT);
        vehicle.setDeclaredVolume(new BigDecimal("25.500"));
        vehicle.setStatus(status);
        vehicle.setArrivedAt(LocalDateTime.of(2026, 4, 14, 8, 0));
        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());
        return vehicle;
    }

    private VehicleResponse buildResponse(VehicleStatus status) {
        return new VehicleResponse(
                VEHICLE_ID, COMPANY_A, BATCH_ID,
                "AA1234BB", "Ivan Petrenko",
                CultureType.WHEAT, new BigDecimal("25.500"),
                status,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private CreateVehicleRequest validCreateRequest(LocalDateTime arrivedAt) {
        return new CreateVehicleRequest(
                BATCH_ID, "AA1234BB", "Ivan Petrenko",
                CultureType.WHEAT, new BigDecimal("25.500"),
                arrivedAt, null
        );
    }

    private UpdateVehicleRequest anyUpdateRequest() {
        return new UpdateVehicleRequest(null, null, null, null, null, null, null, null, null);
    }

    private VehicleFilterRequest emptyFilter() {
        return new VehicleFilterRequest(null, null, null, null);
    }
}
