package com.grainflow.warehouse.controller;

import com.grainflow.warehouse.dto.ApiResponse;
import com.grainflow.warehouse.dto.vehicle.CreateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.UpdateVehicleRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleFilterRequest;
import com.grainflow.warehouse.dto.vehicle.VehicleResponse;
import com.grainflow.warehouse.security.AuthenticatedUser;
import com.grainflow.warehouse.service.VehicleService;
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
@RequestMapping("/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @Valid @RequestBody CreateVehicleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        VehicleResponse response = vehicleService.create(request, user.companyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Vehicle registered"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<VehicleResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateVehicleRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(vehicleService.update(id, request, user.companyId()), "Vehicle updated");
    }

    @GetMapping("/{id}")
    public ApiResponse<VehicleResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(vehicleService.getById(id, user.companyId()));
    }

    @GetMapping
    public ApiResponse<Page<VehicleResponse>> getAll(
            @ModelAttribute VehicleFilterRequest filter,
            @PageableDefault(size = 20, sort = "arrivedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(vehicleService.getAll(user.companyId(), filter, pageable));
    }

    // --- Status transitions ---

    @PatchMapping("/{id}/start-processing")
    public ApiResponse<VehicleResponse> startProcessing(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(vehicleService.startProcessing(id, user.companyId()), "Unloading started");
    }

    @PatchMapping("/{id}/finish-processing")
    public ApiResponse<VehicleResponse> finishProcessing(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(vehicleService.finishProcessing(id, user.companyId()), "Unloading finished");
    }

    @PatchMapping("/{id}/accept")
    public ApiResponse<VehicleResponse> accept(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(vehicleService.accept(id, user.companyId()), "Vehicle accepted");
    }

    @PatchMapping("/{id}/reject")
    public ApiResponse<VehicleResponse> reject(
            @PathVariable UUID id,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {

        return ApiResponse.success(vehicleService.reject(id, comment, user.companyId()), "Vehicle rejected");
    }
}
