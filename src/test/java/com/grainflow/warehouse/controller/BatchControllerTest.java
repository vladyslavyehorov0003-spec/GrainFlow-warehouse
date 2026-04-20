package com.grainflow.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grainflow.warehouse.config.SecurityConfig;
import com.grainflow.warehouse.dto.batch.AddVolumeRequest;
import com.grainflow.warehouse.dto.batch.CreateBatchRequest;
import com.grainflow.warehouse.dto.batch.UpdateBatchRequest;
import com.grainflow.warehouse.entity.BatchStatus;
import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.fixture.BatchTestFixtures;
import com.grainflow.warehouse.security.AuthClient;
import com.grainflow.warehouse.service.BatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.grainflow.warehouse.fixture.BatchTestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BatchController.class)
@Import(SecurityConfig.class)
class BatchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BatchService batchService;

    @MockitoBean
    AuthClient authClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String BASE_URL = "/api/v1/batches";

    // ===================== CREATE BATCH =====================
    @BeforeEach
    void setUp(WebApplicationContext wac) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .defaultRequest(get("/").contextPath("/api/v1")) // Устанавливаем контекст по умолчанию
                .build();
    }

    @Test
    void createBatch_asManagerA_returns201() throws Exception {
        when(batchService.create(any(), eq(COMPANY_A_ID))).thenReturn(sampleBatch());

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.contractNumber").value("CONTRACT-2026-001"));
    }

    @Test
    void createBatch_asManagerB_createsForCompanyB() throws Exception {
        when(batchService.create(any(), eq(COMPANY_B_ID))).thenReturn(
                new com.grainflow.warehouse.dto.batch.BatchResponse(
                        UUID.randomUUID(), COMPANY_B_ID, "CONTRACT-B-001",
                        CultureType.CORN, BatchStatus.PLANNED,
                        new BigDecimal("300.000"), BigDecimal.ZERO, BigDecimal.ZERO,
                        LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1),
                        null,
                        java.time.LocalDateTime.now(), java.time.LocalDateTime.now()
                ));

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.companyId").value(COMPANY_B_ID.toString()));
    }

    @Test
    void createBatch_asWorker_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBatch_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void createBatch_invalidRequest_missingCulture_returns400() throws Exception {
        String body = """
                {
                  "contractNumber": "CONTRACT-001",
                  "totalVolume": 500.0,
                  "loadingFrom": "2026-01-01",
                  "loadingTo": "2026-12-31"
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    // ===================== UPDATE BATCH =====================

    @Test
    void updateBatch_asManagerA_sameCompany_returns200() throws Exception {
        when(batchService.update(eq(BATCH_ID), any(), eq(COMPANY_A_ID))).thenReturn(sampleBatch());

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void updateBatch_asManagerB_otherCompany_returns403() throws Exception {
        when(batchService.update(eq(BATCH_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID)
                        .with(user(managerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateBatch_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateBatch_closedBatch_returns400() throws Exception {
        when(batchService.update(eq(BATCH_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Cannot update a closed batch"));

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot update a closed batch"));
    }

    // ===================== CLOSE BATCH =====================

    @Test
    void closeBatch_asManagerA_sameCompany_returns204() throws Exception {
        doNothing().when(batchService).close(eq(BATCH_ID), eq(COMPANY_A_ID));

        mockMvc.perform(post(BASE_URL + "/" + BATCH_ID + "/close")
                        .with(user(managerA)))
                .andExpect(status().isNoContent());
    }

    @Test
    void closeBatch_asManagerB_otherCompany_returns403() throws Exception {
        doThrow(WarehouseException.forbidden("Access denied"))
                .when(batchService).close(eq(BATCH_ID), eq(COMPANY_B_ID));

        mockMvc.perform(post(BASE_URL + "/" + BATCH_ID + "/close")
                        .with(user(managerB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void closeBatch_asWorkerA2_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL + "/" + BATCH_ID + "/close")
                        .with(user(workerA2)))
                .andExpect(status().isForbidden());
    }

    @Test
    void closeBatch_alreadyClosed_returns400() throws Exception {
        doThrow(WarehouseException.badRequest("Batch is already closed"))
                .when(batchService).close(eq(BATCH_ID), eq(COMPANY_A_ID));

        mockMvc.perform(post(BASE_URL + "/" + BATCH_ID + "/close")
                        .with(user(managerA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Batch is already closed"));
    }

    // ===================== GET BY ID =====================

    @Test
    void getBatchById_asManagerA_sameCompany_returns200() throws Exception {
        when(batchService.getById(eq(BATCH_ID), eq(COMPANY_A_ID))).thenReturn(sampleBatch());

        mockMvc.perform(get(BASE_URL + "/" + BATCH_ID)
                        .with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.data.culture").value("WHEAT"));
    }

    @Test
    void getBatchById_asWorkerA1_sameCompany_returns200() throws Exception {
        when(batchService.getById(eq(BATCH_ID), eq(COMPANY_A_ID))).thenReturn(sampleBatch());

        mockMvc.perform(get(BASE_URL + "/" + BATCH_ID)
                        .with(user(workerA1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contractNumber").value("CONTRACT-2026-001"));
    }

    @Test
    void getBatchById_asWorkerA2_sameCompany_returns200() throws Exception {
        when(batchService.getById(eq(BATCH_ID), eq(COMPANY_A_ID))).thenReturn(sampleBatch());

        mockMvc.perform(get(BASE_URL + "/" + BATCH_ID)
                        .with(user(workerA2)))
                .andExpect(status().isOk());
    }

    @Test
    void getBatchById_asManagerB_otherCompany_returns403() throws Exception {
        when(batchService.getById(eq(BATCH_ID), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(get(BASE_URL + "/" + BATCH_ID)
                        .with(user(managerB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBatchById_asWorkerB1_otherCompany_returns403() throws Exception {
        when(batchService.getById(eq(BATCH_ID), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(get(BASE_URL + "/" + BATCH_ID)
                        .with(user(workerB1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBatchById_notFound_returns404() throws Exception {
        when(batchService.getById(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.notFound("Batch not found"));

        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                        .with(user(managerA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Batch not found"));
    }

    // ===================== GET ALL =====================

    @Test
    void getAllBatches_asManagerA_returns200WithPage() throws Exception {
        when(batchService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleBatch()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE_URL)
                        .with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].contractNumber").value("CONTRACT-2026-001"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getAllBatches_asWorkerA1_returns200WithPage() throws Exception {
        when(batchService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleBatch())));

        mockMvc.perform(get(BASE_URL)
                        .with(user(workerA1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getAllBatches_asManagerB_returnsOnlyCompanyBData() throws Exception {
        when(batchService.getAll(eq(COMPANY_B_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE_URL)
                        .with(user(managerB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void getAllBatches_withFilters_passes() throws Exception {
        when(batchService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(sampleBatch())));

        mockMvc.perform(get(BASE_URL)
                        .with(user(managerA))
                        .param("culture", "WHEAT")
                        .param("status", "PLANNED")
                        .param("contractNumber", "CONTRACT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].culture").value("WHEAT"));
    }

    // ===================== ADD ACCEPTED VOLUME =====================

    @Test
    void addAcceptedVolume_asManagerA_returns200() throws Exception {
        when(batchService.addAcceptedVolume(eq(BATCH_ID), any(), eq(COMPANY_A_ID))).thenReturn(activeBatch());

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID + "/accepted-volume")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddVolumeRequest(new BigDecimal("100.000")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptedVolume").value(100.0));
    }

    @Test
    void addAcceptedVolume_asWorkerA1_returns200() throws Exception {
        when(batchService.addAcceptedVolume(eq(BATCH_ID), any(), eq(COMPANY_A_ID))).thenReturn(activeBatch());

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID + "/accepted-volume")
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddVolumeRequest(new BigDecimal("100.000")))))
                .andExpect(status().isOk());
    }

    @Test
    void addAcceptedVolume_asWorkerB1_otherCompany_returns403() throws Exception {
        when(batchService.addAcceptedVolume(eq(BATCH_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID + "/accepted-volume")
                        .with(user(workerB1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddVolumeRequest(new BigDecimal("100.000")))))
                .andExpect(status().isForbidden());
    }

    @Test
    void addAcceptedVolume_closedBatch_returns400() throws Exception {
        when(batchService.addAcceptedVolume(eq(BATCH_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Cannot add volume to a closed batch"));

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID + "/accepted-volume")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddVolumeRequest(new BigDecimal("100.000")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot add volume to a closed batch"));
    }

    // ===================== ADD UNLOADED VOLUME =====================

    @Test
    void addUnloadedVolume_asManagerA_returns200() throws Exception {
        when(batchService.addUnloadedVolume(eq(BATCH_ID), any(), eq(COMPANY_A_ID))).thenReturn(activeBatch());

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID + "/unloaded-volume")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddVolumeRequest(new BigDecimal("50.000")))))
                .andExpect(status().isOk());
    }

    @Test
    void addUnloadedVolume_asWorkerA2_returns200() throws Exception {
        when(batchService.addUnloadedVolume(eq(BATCH_ID), any(), eq(COMPANY_A_ID))).thenReturn(activeBatch());

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID + "/unloaded-volume")
                        .with(user(workerA2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddVolumeRequest(new BigDecimal("50.000")))))
                .andExpect(status().isOk());
    }

    @Test
    void addUnloadedVolume_exceedsAccepted_returns400() throws Exception {
        when(batchService.addUnloadedVolume(eq(BATCH_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Unloaded volume cannot exceed accepted volume"));

        mockMvc.perform(patch(BASE_URL + "/" + BATCH_ID + "/unloaded-volume")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddVolumeRequest(new BigDecimal("999.000")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unloaded volume cannot exceed accepted volume"));
    }

    // ===================== HELPERS =====================

    private CreateBatchRequest validCreateRequest() {
        return new CreateBatchRequest(
                "CONTRACT-2026-001",
                CultureType.WHEAT,
                new BigDecimal("500.000"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                "test batch"
        );
    }

    private UpdateBatchRequest validUpdateRequest() {
        return new UpdateBatchRequest(
                null, null, null, null, null, null, null, null, "updated comment"
        );
    }
}
