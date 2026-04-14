package com.grainflow.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grainflow.warehouse.config.SecurityConfig;
import com.grainflow.warehouse.dto.vehicle.CreateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.UpdateVehicleRequest;
import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.fixture.VehicleTestFixtures;
import com.grainflow.warehouse.security.AuthClient;
import com.grainflow.warehouse.service.VehicleService;
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
import static com.grainflow.warehouse.fixture.VehicleTestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VehicleController.class)
@Import(SecurityConfig.class)
class VehicleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean VehicleService vehicleService;
    @MockitoBean AuthClient authClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String BASE_URL = "/api/v1/vehicles";

    // ===================== CREATE =====================

    @Test
    void create_asManagerA_returns201() throws Exception {
        when(vehicleService.create(any(), eq(COMPANY_A_ID))).thenReturn(arrivedVehicle());

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.licensePlate").value("AA1234BB"))
                .andExpect(jsonPath("$.data.status").value("ARRIVED"));
    }

    @Test
    void create_asWorkerA1_returns201() throws Exception {
        when(vehicleService.create(any(), eq(COMPANY_A_ID))).thenReturn(arrivedVehicle());

        mockMvc.perform(post(BASE_URL)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_missingLicensePlate_returns400() throws Exception {
        String body = """
                {
                  "batchId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
                  "culture": "WHEAT",
                  "declaredVolume": 25.5
                }
                """;

        mockMvc.perform(post(BASE_URL)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void create_closedBatch_returns400() throws Exception {
        when(vehicleService.create(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Cannot register vehicle for a closed batch"));

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot register vehicle for a closed batch"));
    }

    @Test
    void create_batchFromOtherCompany_returns403() throws Exception {
        when(vehicleService.create(any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    // ===================== UPDATE =====================

    @Test
    void update_asManagerA_returns200() throws Exception {
        when(vehicleService.update(eq(VEHICLE_ID), any(), eq(COMPANY_A_ID))).thenReturn(arrivedVehicle());

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void update_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_asManagerB_otherCompany_returns403() throws Exception {
        when(vehicleService.update(eq(VEHICLE_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID)
                        .with(user(managerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_finalizedVehicle_returns400() throws Exception {
        when(vehicleService.update(eq(VEHICLE_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Cannot update a finalized vehicle"));

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdateRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot update a finalized vehicle"));
    }

    // ===================== GET BY ID =====================

    @Test
    void getById_asManagerA_returns200() throws Exception {
        when(vehicleService.getById(eq(VEHICLE_ID), eq(COMPANY_A_ID))).thenReturn(arrivedVehicle());

        mockMvc.perform(get(BASE_URL + "/" + VEHICLE_ID)
                        .with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(VEHICLE_ID.toString()))
                .andExpect(jsonPath("$.data.licensePlate").value("AA1234BB"));
    }

    @Test
    void getById_asWorkerA2_returns200() throws Exception {
        when(vehicleService.getById(eq(VEHICLE_ID), eq(COMPANY_A_ID))).thenReturn(arrivedVehicle());

        mockMvc.perform(get(BASE_URL + "/" + VEHICLE_ID)
                        .with(user(workerA2)))
                .andExpect(status().isOk());
    }

    @Test
    void getById_asWorkerB1_otherCompany_returns403() throws Exception {
        when(vehicleService.getById(eq(VEHICLE_ID), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(get(BASE_URL + "/" + VEHICLE_ID)
                        .with(user(workerB1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(vehicleService.getById(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.notFound("Vehicle not found"));

        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                        .with(user(managerA)))
                .andExpect(status().isNotFound());
    }

    // ===================== GET ALL =====================

    @Test
    void getAll_asManagerA_returns200() throws Exception {
        when(vehicleService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(arrivedVehicle()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get(BASE_URL)
                        .with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].licensePlate").value("AA1234BB"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getAll_asWorkerB1_returnsOnlyCompanyBData() throws Exception {
        when(vehicleService.getAll(eq(COMPANY_B_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE_URL)
                        .with(user(workerB1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void getAll_withFilters_passes() throws Exception {
        when(vehicleService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(arrivedVehicle())));

        mockMvc.perform(get(BASE_URL)
                        .with(user(managerA))
                        .param("status", "ARRIVED")
                        .param("culture", "WHEAT")
                        .param("licensePlate", "AA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("ARRIVED"));
    }

    // ===================== STATUS TRANSITIONS =====================

    @Test
    void startProcessing_asWorkerA1_returns200() throws Exception {
        when(vehicleService.startProcessing(eq(VEHICLE_ID), eq(COMPANY_A_ID))).thenReturn(inProcessVehicle());

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/start-processing")
                        .with(user(workerA1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROCESS"));
    }

    @Test
    void startProcessing_asManagerA_returns200() throws Exception {
        when(vehicleService.startProcessing(eq(VEHICLE_ID), eq(COMPANY_A_ID))).thenReturn(inProcessVehicle());

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/start-processing")
                        .with(user(managerA)))
                .andExpect(status().isOk());
    }

    @Test
    void startProcessing_notArrived_returns400() throws Exception {
        when(vehicleService.startProcessing(eq(VEHICLE_ID), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Vehicle must be in ARRIVED status to start processing"));

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/start-processing")
                        .with(user(workerA1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Vehicle must be in ARRIVED status to start processing"));
    }

    @Test
    void finishProcessing_asWorkerA2_returns200() throws Exception {
        when(vehicleService.finishProcessing(eq(VEHICLE_ID), eq(COMPANY_A_ID))).thenReturn(inProcessVehicle());

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/finish-processing")
                        .with(user(workerA2)))
                .andExpect(status().isOk());
    }

    @Test
    void accept_asManagerA_returns200() throws Exception {
        when(vehicleService.accept(eq(VEHICLE_ID), eq(COMPANY_A_ID))).thenReturn(acceptedVehicle());

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/accept")
                        .with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void accept_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/accept")
                        .with(user(workerA1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void accept_notInProcess_returns400() throws Exception {
        when(vehicleService.accept(eq(VEHICLE_ID), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Vehicle must be in IN_PROCESS status to be accepted"));

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/accept")
                        .with(user(managerA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reject_asManagerA_withComment_returns200() throws Exception {
        when(vehicleService.reject(eq(VEHICLE_ID), eq("moisture too high"), eq(COMPANY_A_ID)))
                .thenReturn(rejectedVehicle());

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/reject")
                        .param("comment", "moisture too high")
                        .with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.comment").value("moisture too high"));
    }

    @Test
    void reject_asManagerA_noComment_returns200() throws Exception {
        when(vehicleService.reject(eq(VEHICLE_ID), eq(null), eq(COMPANY_A_ID)))
                .thenReturn(rejectedVehicle());

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/reject")
                        .with(user(managerA)))
                .andExpect(status().isOk());
    }

    @Test
    void reject_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/reject")
                        .with(user(workerA1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void reject_asManagerB_otherCompany_returns403() throws Exception {
        when(vehicleService.reject(eq(VEHICLE_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + VEHICLE_ID + "/reject")
                        .with(user(managerB)))
                .andExpect(status().isForbidden());
    }

    // ===================== HELPERS =====================

    private CreateVehicleRequest validCreateRequest() {
        return new CreateVehicleRequest(
                BATCH_ID, "AA1234BB", "Ivan Petrenko",
                CultureType.WHEAT, new BigDecimal("25.500"),
                null, null
        );
    }

    private UpdateVehicleRequest validUpdateRequest() {
        return new UpdateVehicleRequest(
                null, "New Driver", null, null, null, null, null, null, "updated"
        );
    }
}
