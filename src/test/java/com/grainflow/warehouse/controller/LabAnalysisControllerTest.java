package com.grainflow.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grainflow.warehouse.config.SecurityConfig;
import com.grainflow.warehouse.dto.lab.*;
import com.grainflow.warehouse.entity.LabStatus;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.security.AuthClient;
import com.grainflow.warehouse.service.LabAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.grainflow.warehouse.fixture.BatchTestFixtures.*;
import static com.grainflow.warehouse.fixture.LabAnalysisTestFixtures.*;
import static com.grainflow.warehouse.fixture.VehicleTestFixtures.VEHICLE_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LabAnalysisController.class)
@Import(SecurityConfig.class)
class LabAnalysisControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LabAnalysisService labAnalysisService;
    @MockitoBean AuthClient authClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String BASE_URL = "/api/v1/lab-analyses";

    // ===================== CREATE =====================

    @Test
    void create_asManagerA_returns201() throws Exception {
        when(labAnalysisService.create(any(), eq(COMPANY_A_ID))).thenReturn(pendingLab());

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLabAnalysisRequest(VEHICLE_ID, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void create_asWorkerA1_returns201() throws Exception {
        when(labAnalysisService.create(any(), eq(COMPANY_A_ID))).thenReturn(pendingLab());

        mockMvc.perform(post(BASE_URL)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLabAnalysisRequest(VEHICLE_ID, null))))
                .andExpect(status().isCreated());
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLabAnalysisRequest(VEHICLE_ID, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_missingVehicleId_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateAnalysis_returns409() throws Exception {
        when(labAnalysisService.create(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.conflict("Lab analysis already exists for vehicle: " + VEHICLE_ID));

        mockMvc.perform(post(BASE_URL)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLabAnalysisRequest(VEHICLE_ID, null))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_rejectedVehicle_returns400() throws Exception {
        when(labAnalysisService.create(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Cannot create lab analysis for a rejected vehicle"));

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateLabAnalysisRequest(VEHICLE_ID, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot create lab analysis for a rejected vehicle"));
    }

    // ===================== UPDATE =====================

    @Test
    void update_asManagerA_returns200() throws Exception {
        when(labAnalysisService.update(eq(LAB_ID), any(), eq(COMPANY_A_ID))).thenReturn(inProgressLab());

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void update_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_asManagerB_otherCompany_returns403() throws Exception {
        when(labAnalysisService.update(eq(LAB_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID)
                        .with(user(managerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_finalizedLab_returns400() throws Exception {
        when(labAnalysisService.update(eq(LAB_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Cannot update a finalized lab analysis"));

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot update a finalized lab analysis"));
    }

    // ===================== GET BY ID =====================

    @Test
    void getById_asManagerA_returns200() throws Exception {
        when(labAnalysisService.getById(eq(LAB_ID), eq(COMPANY_A_ID))).thenReturn(pendingLab());

        mockMvc.perform(get(BASE_URL + "/" + LAB_ID).with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(LAB_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void getById_asWorkerA1_returns200() throws Exception {
        when(labAnalysisService.getById(eq(LAB_ID), eq(COMPANY_A_ID))).thenReturn(inProgressLab());

        mockMvc.perform(get(BASE_URL + "/" + LAB_ID).with(user(workerA1)))
                .andExpect(status().isOk());
    }

    @Test
    void getById_asWorkerB1_otherCompany_returns403() throws Exception {
        when(labAnalysisService.getById(eq(LAB_ID), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(get(BASE_URL + "/" + LAB_ID).with(user(workerB1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(labAnalysisService.getById(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.notFound("Lab analysis not found"));

        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()).with(user(managerA)))
                .andExpect(status().isNotFound());
    }

    // ===================== GET ALL =====================

    @Test
    void getAll_asManagerA_returns200() throws Exception {
        when(labAnalysisService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(pendingLab(), inProgressLab()), PageRequest.of(0, 20), 2));

        mockMvc.perform(get(BASE_URL).with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getAll_asWorkerB1_returnsOnlyCompanyBData() throws Exception {
        when(labAnalysisService.getAll(eq(COMPANY_B_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE_URL).with(user(workerB1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void getAll_withFilters_passes() throws Exception {
        when(labAnalysisService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(pendingLab())));

        mockMvc.perform(get(BASE_URL)
                        .with(user(managerA))
                        .param("status", "PENDING")
                        .param("vehicleId", VEHICLE_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }

    // ===================== START =====================

    @Test
    void start_asWorkerA1_returns200() throws Exception {
        when(labAnalysisService.start(eq(LAB_ID), eq(COMPANY_A_ID))).thenReturn(inProgressLab());

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/start").with(user(workerA1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void start_asManagerA_returns200() throws Exception {
        when(labAnalysisService.start(eq(LAB_ID), eq(COMPANY_A_ID))).thenReturn(inProgressLab());

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/start").with(user(managerA)))
                .andExpect(status().isOk());
    }

    @Test
    void start_notPending_returns400() throws Exception {
        when(labAnalysisService.start(eq(LAB_ID), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Analysis must be in PENDING status to start"));

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/start").with(user(workerA1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Analysis must be in PENDING status to start"));
    }

    // ===================== START DRYING =====================

    @Test
    void startDrying_asWorkerA1_returns200() throws Exception {
        when(labAnalysisService.startDrying(eq(LAB_ID), any(), eq(COMPANY_A_ID))).thenReturn(dryingLab());

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/start-drying")
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartDryingRequest(new BigDecimal("25.500"), null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.volumeBeforeDrying").value(25.5));
    }

    @Test
    void startDrying_missingVolume_returns400() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/start-drying")
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startDrying_alreadyStarted_returns400() throws Exception {
        when(labAnalysisService.startDrying(eq(LAB_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Drying has already been started"));

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/start-drying")
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StartDryingRequest(new BigDecimal("25.500"), null))))
                .andExpect(status().isBadRequest());
    }

    // ===================== FINISH DRYING =====================

    @Test
    void finishDrying_asWorkerA2_returns200() throws Exception {
        when(labAnalysisService.finishDrying(eq(LAB_ID), any(), eq(COMPANY_A_ID))).thenReturn(inProgressLab());

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-drying")
                        .with(user(workerA2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00")))))
                .andExpect(status().isOk());
    }

    @Test
    void finishDrying_dryingNotStarted_returns400() throws Exception {
        when(labAnalysisService.finishDrying(eq(LAB_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Drying has not been started yet"));

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-drying")
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FinishDryingRequest(new BigDecimal("24.800"), new BigDecimal("13.00")))))
                .andExpect(status().isBadRequest());
    }

    // ===================== FINISH ANALYSIS =====================

    @Test
    void finishAnalysis_asManagerA_passed_returns200() throws Exception {
        when(labAnalysisService.finishAnalysis(eq(LAB_ID), any(), eq(COMPANY_A_ID))).thenReturn(passedLab());

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-analysis")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFinishRequest(LabStatus.PASSED, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PASSED"));
    }

    @Test
    void finishAnalysis_asManagerA_failed_withComment_returns200() throws Exception {
        when(labAnalysisService.finishAnalysis(eq(LAB_ID), any(), eq(COMPANY_A_ID))).thenReturn(failedLab());

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-analysis")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFinishRequest(LabStatus.FAILED, "moisture too high"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.comment").value("moisture too high"));
    }

    @Test
    void finishAnalysis_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-analysis")
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFinishRequest(LabStatus.PASSED, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void finishAnalysis_asManagerB_otherCompany_returns403() throws Exception {
        when(labAnalysisService.finishAnalysis(eq(LAB_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-analysis")
                        .with(user(managerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFinishRequest(LabStatus.PASSED, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void finishAnalysis_notInProgress_returns400() throws Exception {
        when(labAnalysisService.finishAnalysis(eq(LAB_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Analysis must be in IN_PROGRESS status to finish"));

        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-analysis")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validFinishRequest(LabStatus.PASSED, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finishAnalysis_missingRequiredFields_returns400() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + LAB_ID + "/finish-analysis")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ===================== HELPERS =====================

    private FinishAnalysisRequest validFinishRequest(LabStatus status, String comment) {
        return new FinishAnalysisRequest(
                new BigDecimal("14.50"),
                new BigDecimal("1.20"),
                new BigDecimal("12.00"),
                new BigDecimal("24.800"),
                status,
                comment
        );
    }
}
