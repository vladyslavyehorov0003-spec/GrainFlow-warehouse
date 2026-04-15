package com.grainflow.warehouse.controller;

import com.grainflow.warehouse.dto.ApiResponse;
import com.grainflow.warehouse.dto.lab.*;
import com.grainflow.warehouse.security.AuthenticatedUser;
import com.grainflow.warehouse.service.LabAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/lab-analyses")
@RequiredArgsConstructor
public class LabAnalysisController {

    private final LabAnalysisService labAnalysisService;

    @PostMapping
    public ResponseEntity<ApiResponse<LabAnalysisResponse>> create(
            @Valid @RequestBody CreateLabAnalysisRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        LabAnalysisResponse response = labAnalysisService.create(request, user.companyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Lab analysis created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<LabAnalysisResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLabAnalysisRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(labAnalysisService.update(id, request, user.companyId()), "Lab analysis updated");
    }

    @GetMapping("/{id}")
    public ApiResponse<LabAnalysisResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(labAnalysisService.getById(id, user.companyId()));
    }

    @GetMapping
    public ApiResponse<Page<LabAnalysisResponse>> getAll(
            @ModelAttribute LabAnalysisFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(labAnalysisService.getAll(user.companyId(), filter, pageable));
    }

    // --- Status transitions ---

    @PatchMapping("/{id}/start")
    public ApiResponse<LabAnalysisResponse> start(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(labAnalysisService.start(id, user.companyId()), "Analysis started");
    }

    @PatchMapping("/{id}/start-drying")
    public ApiResponse<LabAnalysisResponse> startDrying(
            @PathVariable UUID id,
            @Valid @RequestBody StartDryingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(labAnalysisService.startDrying(id, request, user.companyId()), "Drying started");
    }

    @PatchMapping("/{id}/finish-drying")
    public ApiResponse<LabAnalysisResponse> finishDrying(
            @PathVariable UUID id,
            @Valid @RequestBody FinishDryingRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(labAnalysisService.finishDrying(id, request, user.companyId()), "Drying finished");
    }

    @PatchMapping("/{id}/finish-analysis")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<LabAnalysisResponse> finishAnalysis(
            @PathVariable UUID id,
            @Valid @RequestBody FinishAnalysisRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(labAnalysisService.finishAnalysis(id, request, user.companyId()), "Analysis finished");
    }
}
