package com.grainflow.warehouse.service;

import com.grainflow.warehouse.dto.batch.AddVolumeRequest;
import com.grainflow.warehouse.dto.batch.BatchFilterRequest;
import com.grainflow.warehouse.dto.batch.BatchResponse;
import com.grainflow.warehouse.dto.batch.CreateBatchRequest;
import com.grainflow.warehouse.dto.batch.UpdateBatchRequest;
import com.grainflow.warehouse.entity.Batch;
import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.mapper.BatchMapper;
import com.grainflow.warehouse.repository.BatchRepository;
import com.grainflow.warehouse.service.impl.BatchServiceImpl;
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
class BatchServiceImplTest {

    @Mock BatchRepository batchRepository;
    @Mock BatchMapper batchMapper;
    @InjectMocks BatchServiceImpl batchService;

    // --- Fixed IDs ---
    private static final UUID COMPANY_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID COMPANY_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BATCH_ID  = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    // ===================== CREATE =====================

    @Test
    void create_success_returnsBatchResponse() {
        CreateBatchRequest request = createRequest("CONTRACT-001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Batch entity = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        BatchResponse response = buildResponse(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);

        when(batchRepository.existsByContractNumberAndCompanyId("CONTRACT-001", COMPANY_A)).thenReturn(false);
        when(batchMapper.toEntity(request, COMPANY_A)).thenReturn(entity);
        when(batchRepository.save(entity)).thenReturn(entity);
        when(batchMapper.toResponseDto(entity)).thenReturn(response);

        BatchResponse result = batchService.create(request, COMPANY_A);

        assertThat(result).isEqualTo(response);
        verify(batchRepository).save(entity);
    }

    @Test
    void create_loadingToBeforeLoadingFrom_throwsBadRequest() {
        CreateBatchRequest request = createRequest("CONTRACT-001", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> batchService.create(request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void create_duplicateContractNumber_throwsConflict() {
        CreateBatchRequest request = createRequest("CONTRACT-001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        when(batchRepository.existsByContractNumberAndCompanyId("CONTRACT-001", COMPANY_A)).thenReturn(true);

        assertThatThrownBy(() -> batchService.create(request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void create_sameContractNumberDifferentCompany_succeeds() {
        CreateBatchRequest request = createRequest("CONTRACT-001", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        Batch entity = buildBatch(COMPANY_B, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        BatchResponse response = buildResponse(COMPANY_B, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);

        // Same contract number but different company — no conflict
        when(batchRepository.existsByContractNumberAndCompanyId("CONTRACT-001", COMPANY_B)).thenReturn(false);
        when(batchMapper.toEntity(request, COMPANY_B)).thenReturn(entity);
        when(batchRepository.save(entity)).thenReturn(entity);
        when(batchMapper.toResponseDto(entity)).thenReturn(response);

        assertThatCode(() -> batchService.create(request, COMPANY_B)).doesNotThrowAnyException();
    }

    // ===================== UPDATE =====================

    @Test
    void update_success_returnsUpdatedResponse() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        UpdateBatchRequest request = new UpdateBatchRequest(null, null, null, null, null, null, null, null, "new comment");
        BatchResponse response = buildResponse(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(batch)).thenReturn(batch);
        when(batchMapper.toResponseDto(batch)).thenReturn(response);

        BatchResponse result = batchService.update(BATCH_ID, request, COMPANY_A);

        assertThat(result).isEqualTo(response);
        verify(batchMapper).updateBatchFromDto(batch, request);
    }

    @Test
    void update_batchNotFound_throwsNotFound() {
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.update(BATCH_ID, anyUpdateRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void update_otherCompany_throwsForbidden() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.update(BATCH_ID, anyUpdateRequest(), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void update_closedBatch_throwsBadRequest() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.CLOSED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.update(BATCH_ID, anyUpdateRequest(), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void update_duplicateContractNumber_throwsConflict() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        batch.setContractNumber("OLD-CONTRACT");

        UpdateBatchRequest request = new UpdateBatchRequest("NEW-CONTRACT", null, null, null, null, null, null, null, null);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.existsByContractNumberAndCompanyId("NEW-CONTRACT", COMPANY_A)).thenReturn(true);

        assertThatThrownBy(() -> batchService.update(BATCH_ID, request, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void update_sameContractNumber_noConflictCheck() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        batch.setContractNumber("SAME-CONTRACT");

        UpdateBatchRequest request = new UpdateBatchRequest("SAME-CONTRACT", null, null, null, null, null, null, null, null);
        BatchResponse response = buildResponse(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(batch)).thenReturn(batch);
        when(batchMapper.toResponseDto(batch)).thenReturn(response);

        batchService.update(BATCH_ID, request, COMPANY_A);

        // No conflict check when contract number hasn't changed
        verify(batchRepository, never()).existsByContractNumberAndCompanyId(any(), any());
    }

    // ===================== CLOSE =====================

    @Test
    void close_plannedBatch_success() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        batchService.close(BATCH_ID, COMPANY_A);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.CLOSED);
        verify(batchRepository).save(batch);
    }

    @Test
    void close_activeBatch_success() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("100.000"), BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        batchService.close(BATCH_ID, COMPANY_A);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.CLOSED);
    }

    @Test
    void close_alreadyClosed_throwsBadRequest() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.CLOSED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.close(BATCH_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void close_otherCompany_throwsForbidden() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.close(BATCH_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== GET BY ID =====================

    @Test
    void getById_success_returnsMappedResponse() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        BatchResponse response = buildResponse(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchMapper.toResponseDto(batch)).thenReturn(response);

        BatchResponse result = batchService.getById(BATCH_ID, COMPANY_A);

        assertThat(result).isEqualTo(response);
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> batchService.getById(BATCH_ID, COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getById_otherCompany_throwsForbidden() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.getById(BATCH_ID, COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== GET ALL =====================

    @Test
    void getAll_returnsPageOfResponses() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        BatchResponse response = buildResponse(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        PageRequest pageable = PageRequest.of(0, 20);

        when(batchRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(batch)));
        when(batchMapper.toResponseDto(batch)).thenReturn(response);

        Page<BatchResponse> result = batchService.getAll(COMPANY_A, emptyFilter(), pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(response);
    }

    @Test
    void getAll_emptyResult_returnsEmptyPage() {
        when(batchRepository.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<BatchResponse> result = batchService.getAll(COMPANY_A, emptyFilter(), PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
    }

    // ===================== ADD ACCEPTED VOLUME =====================

    @Test
    void addAcceptedVolume_plannedBatch_addsVolumeAndActivates() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.PLANNED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(batch)).thenReturn(batch);
        when(batchMapper.toResponseDto(batch)).thenReturn(buildResponse(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("100.000"), BigDecimal.ZERO));

        batchService.addAcceptedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("100.000")), COMPANY_A);

        assertThat(batch.getAcceptedVolume()).isEqualByComparingTo("100.000");
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.ACTIVE);
    }

    @Test
    void addAcceptedVolume_activeBatch_accumulates() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("100.000"), BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(batch)).thenReturn(batch);
        when(batchMapper.toResponseDto(batch)).thenReturn(buildResponse(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("150.000"), BigDecimal.ZERO));

        batchService.addAcceptedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("50.000")), COMPANY_A);

        assertThat(batch.getAcceptedVolume()).isEqualByComparingTo("150.000");
        // Already ACTIVE — stays ACTIVE
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.ACTIVE);
    }

    @Test
    void addAcceptedVolume_closedBatch_throwsBadRequest() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.CLOSED, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.addAcceptedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("100.000")), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addAcceptedVolume_otherCompany_throwsForbidden() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE, BigDecimal.ZERO, BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.addAcceptedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("100.000")), COMPANY_B))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ===================== ADD UNLOADED VOLUME =====================

    @Test
    void addUnloadedVolume_success_accumulates() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("500.000"), new BigDecimal("100.000"));
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(batch)).thenReturn(batch);
        when(batchMapper.toResponseDto(batch)).thenReturn(buildResponse(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("500.000"), new BigDecimal("150.000")));

        batchService.addUnloadedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("50.000")), COMPANY_A);

        assertThat(batch.getUnloadedVolume()).isEqualByComparingTo("150.000");
    }

    @Test
    void addUnloadedVolume_equalsAccepted_succeeds() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("100.000"), BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(batchRepository.save(batch)).thenReturn(batch);
        when(batchMapper.toResponseDto(batch)).thenReturn(buildResponse(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("100.000"), new BigDecimal("100.000")));

        // Exactly equal to accepted — should pass
        assertThatCode(() -> batchService.addUnloadedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("100.000")), COMPANY_A))
                .doesNotThrowAnyException();
    }

    @Test
    void addUnloadedVolume_exceedsAccepted_throwsBadRequest() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.ACTIVE, new BigDecimal("100.000"), BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.addUnloadedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("100.001")), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addUnloadedVolume_closedBatch_throwsBadRequest() {
        Batch batch = buildBatch(COMPANY_A, BatchStatus.CLOSED, new BigDecimal("500.000"), BigDecimal.ZERO);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));

        assertThatThrownBy(() -> batchService.addUnloadedVolume(BATCH_ID, new AddVolumeRequest(new BigDecimal("50.000")), COMPANY_A))
                .isInstanceOf(WarehouseException.class)
                .extracting(e -> ((WarehouseException) e).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ===================== HELPERS =====================

    private Batch buildBatch(UUID companyId, BatchStatus status, BigDecimal accepted, BigDecimal unloaded) {
        Batch batch = new Batch();
        batch.setId(BATCH_ID);
        batch.setCompanyId(companyId);
        batch.setContractNumber("CONTRACT-001");
        batch.setCulture(CultureType.WHEAT);
        batch.setStatus(status);
        batch.setTotalVolume(new BigDecimal("500.000"));
        batch.setAcceptedVolume(accepted);
        batch.setUnloadedVolume(unloaded);
        batch.setLoadingFrom(LocalDate.of(2026, 1, 1));
        batch.setLoadingTo(LocalDate.of(2026, 12, 31));
        batch.setCreatedAt(LocalDateTime.now());
        batch.setUpdatedAt(LocalDateTime.now());
        return batch;
    }

    private BatchResponse buildResponse(UUID companyId, BatchStatus status, BigDecimal accepted, BigDecimal unloaded) {
        return new BatchResponse(
                BATCH_ID, companyId, "CONTRACT-001", CultureType.WHEAT, status,
                new BigDecimal("500.000"), accepted, unloaded,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                null, LocalDateTime.now(), LocalDateTime.now()
        );
    }

    private CreateBatchRequest createRequest(String contractNumber, LocalDate from, LocalDate to) {
        return new CreateBatchRequest(contractNumber, CultureType.WHEAT, new BigDecimal("500.000"), from, to, null);
    }

    private UpdateBatchRequest anyUpdateRequest() {
        return new UpdateBatchRequest(null, null, null, null, null, null, null, null, null);
    }

    private BatchFilterRequest emptyFilter() {
        return new BatchFilterRequest(null, null, null, null, null);
    }
}
