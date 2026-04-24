package com.grainflow.warehouse.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.grainflow.warehouse.config.SecurityConfig;
import com.grainflow.warehouse.dto.silo.*;
import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.exception.WarehouseException;
import com.grainflow.warehouse.security.AuthClient;
import com.grainflow.warehouse.security.ValidateResponse;
import com.grainflow.warehouse.service.SiloService;
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
import java.util.List;
import java.util.UUID;

import static com.grainflow.warehouse.fixture.BatchTestFixtures.*;
import static com.grainflow.warehouse.fixture.LabAnalysisTestFixtures.LAB_ID;
import static com.grainflow.warehouse.fixture.SiloTestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SiloController.class)
@Import(SecurityConfig.class)
class SiloControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SiloService siloService;
    @MockitoBean AuthClient authClient;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String BASE_URL = "/api/v1/silos";

    @BeforeEach
    void setUp(WebApplicationContext wac) {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(wac)
                .apply(springSecurity())
                .defaultRequest(get("/").contextPath("/api/v1")) // Устанавливаем контекст по умолчанию
                .build();
    }
    // ===================== SUBSCRIPTION =====================

    @Test
    void anyWrite_withInactiveSubscription_returns402() throws Exception {
        when(authClient.validate(any())).thenReturn(
                new ValidateResponse(true, managerA.userId(), managerA.companyId(), managerA.email(), "MANAGER", "INACTIVE")
        );

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer fake-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isPaymentRequired());
    }

    // ===================== CREATE =====================

    @Test
    void create_asManagerA_returns201() throws Exception {
        when(siloService.create(any(), eq(COMPANY_A_ID))).thenReturn(emptySilo());

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.name").value("Silo-A1"))
                .andExpect(jsonPath("$.data.currentAmount").value(0));
    }

    @Test
    void create_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxAmount\": 500.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_duplicateName_returns409() throws Exception {
        when(siloService.create(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.conflict("Silo with this name already exists: Silo-A1"));

        mockMvc.perform(post(BASE_URL)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Silo with this name already exists: Silo-A1"));
    }

    // ===================== UPDATE =====================

    @Test
    void update_asManagerA_returns200() throws Exception {
        when(siloService.update(eq(SILO_ID), any(), eq(COMPANY_A_ID))).thenReturn(siloWithGrain());

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void update_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID)
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_asManagerB_otherCompany_returns403() throws Exception {
        when(siloService.update(eq(SILO_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID)
                        .with(user(managerB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_maxAmountLessThanCurrent_returns400() throws Exception {
        when(siloService.update(eq(SILO_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Max amount cannot be less than current amount"));

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID)
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxAmount\": 10.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Max amount cannot be less than current amount"));
    }

    // ===================== DELETE =====================

    @Test
    void delete_asManagerA_emptySilo_returns204() throws Exception {
        doNothing().when(siloService).delete(eq(SILO_ID), eq(COMPANY_A_ID));

        mockMvc.perform(delete(BASE_URL + "/" + SILO_ID).with(user(managerA)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asWorkerA1_returns403() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/" + SILO_ID).with(user(workerA1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_asManagerB_otherCompany_returns403() throws Exception {
        doThrow(WarehouseException.forbidden("Access denied"))
                .when(siloService).delete(eq(SILO_ID), eq(COMPANY_B_ID));

        mockMvc.perform(delete(BASE_URL + "/" + SILO_ID).with(user(managerB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_siloWithGrain_returns400() throws Exception {
        doThrow(WarehouseException.badRequest("Cannot delete silo with grain inside"))
                .when(siloService).delete(eq(SILO_ID), eq(COMPANY_A_ID));

        mockMvc.perform(delete(BASE_URL + "/" + SILO_ID).with(user(managerA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete silo with grain inside"));
    }

    // ===================== GET BY ID =====================

    @Test
    void getById_asManagerA_returns200() throws Exception {
        when(siloService.getById(eq(SILO_ID), eq(COMPANY_A_ID))).thenReturn(siloWithGrain());

        mockMvc.perform(get(BASE_URL + "/" + SILO_ID).with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(SILO_ID.toString()))
                .andExpect(jsonPath("$.data.currentAmount").value(100.0));
    }

    @Test
    void getById_asWorkerA1_returns200() throws Exception {
        when(siloService.getById(eq(SILO_ID), eq(COMPANY_A_ID))).thenReturn(emptySilo());

        mockMvc.perform(get(BASE_URL + "/" + SILO_ID).with(user(workerA1)))
                .andExpect(status().isOk());
    }

    @Test
    void getById_asWorkerB1_otherCompany_returns403() throws Exception {
        when(siloService.getById(eq(SILO_ID), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(get(BASE_URL + "/" + SILO_ID).with(user(workerB1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(siloService.getById(any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.notFound("Silo not found"));

        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID()).with(user(managerA)))
                .andExpect(status().isNotFound());
    }

    // ===================== GET ALL =====================

    @Test
    void getAll_asManagerA_returns200() throws Exception {
        when(siloService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(emptySilo(), siloWithGrain()), PageRequest.of(0, 20), 2));

        mockMvc.perform(get(BASE_URL).with(user(managerA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getAll_asWorkerA2_returns200() throws Exception {
        when(siloService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(emptySilo())));

        mockMvc.perform(get(BASE_URL).with(user(workerA2)))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_asManagerB_returnsOnlyCompanyB() throws Exception {
        when(siloService.getAll(eq(COMPANY_B_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(siloCompanyB())));

        mockMvc.perform(get(BASE_URL).with(user(managerB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].companyId").value(COMPANY_B_ID.toString()));
    }

    @Test
    void getAll_withFilters_passes() throws Exception {
        when(siloService.getAll(eq(COMPANY_A_ID), any(), any()))
                .thenReturn(new PageImpl<>(List.of(siloWithGrain())));

        mockMvc.perform(get(BASE_URL)
                        .with(user(managerA))
                        .param("culture", "WHEAT")
                        .param("name", "Silo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].culture").value("WHEAT"));
    }

    // ===================== ADD GRAIN =====================

    @Test
    void addGrain_asManagerA_returns200() throws Exception {
        when(siloService.addGrain(eq(SILO_ID), any(), eq(COMPANY_A_ID))).thenReturn(siloWithGrain());

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/add-grain")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddGrainRequest(LAB_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentAmount").value(100.0))
                .andExpect(jsonPath("$.data.culture").value("WHEAT"));
    }

    @Test
    void addGrain_asWorkerA1_returns200() throws Exception {
        when(siloService.addGrain(eq(SILO_ID), any(), eq(COMPANY_A_ID))).thenReturn(siloWithGrain());

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/add-grain")
                        .with(user(workerA1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddGrainRequest(LAB_ID))))
                .andExpect(status().isOk());
    }

    @Test
    void addGrain_labNotPassed_returns400() throws Exception {
        when(siloService.addGrain(eq(SILO_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Lab analysis must be PASSED to add grain to silo"));

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/add-grain")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddGrainRequest(LAB_ID))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Lab analysis must be PASSED to add grain to silo"));
    }

    @Test
    void addGrain_notEnoughCapacity_returns400() throws Exception {
        when(siloService.addGrain(eq(SILO_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Not enough capacity. Available: 10.000 tonnes"));

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/add-grain")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddGrainRequest(LAB_ID))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addGrain_missingLabAnalysisId_returns400() throws Exception {
        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/add-grain")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ===================== REMOVE GRAIN =====================

    @Test
    void removeGrain_asManagerA_returns200() throws Exception {
        when(siloService.removeGrain(eq(SILO_ID), any(), eq(COMPANY_A_ID))).thenReturn(emptySilo());

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/remove-grain")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RemoveGrainRequest(new BigDecimal("100.000")))))
                .andExpect(status().isOk());
    }

    @Test
    void removeGrain_asWorkerA2_returns200() throws Exception {
        when(siloService.removeGrain(eq(SILO_ID), any(), eq(COMPANY_A_ID))).thenReturn(emptySilo());

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/remove-grain")
                        .with(user(workerA2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RemoveGrainRequest(new BigDecimal("100.000")))))
                .andExpect(status().isOk());
    }

    @Test
    void removeGrain_notEnoughGrain_returns400() throws Exception {
        when(siloService.removeGrain(eq(SILO_ID), any(), eq(COMPANY_A_ID)))
                .thenThrow(WarehouseException.badRequest("Not enough grain. Current amount: 50.000 tonnes"));

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/remove-grain")
                        .with(user(managerA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RemoveGrainRequest(new BigDecimal("100.000")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Not enough grain. Current amount: 50.000 tonnes"));
    }

    @Test
    void removeGrain_asWorkerB1_otherCompany_returns403() throws Exception {
        when(siloService.removeGrain(eq(SILO_ID), any(), eq(COMPANY_B_ID)))
                .thenThrow(WarehouseException.forbidden("Access denied"));

        mockMvc.perform(patch(BASE_URL + "/" + SILO_ID + "/remove-grain")
                        .with(user(workerB1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RemoveGrainRequest(new BigDecimal("100.000")))))
                .andExpect(status().isForbidden());
    }

    // ===================== HELPERS =====================

    private CreateSiloRequest validCreateRequest() {
        return new CreateSiloRequest("Silo-A1", new BigDecimal("500.000"), CultureType.OATS, null);
    }
}
