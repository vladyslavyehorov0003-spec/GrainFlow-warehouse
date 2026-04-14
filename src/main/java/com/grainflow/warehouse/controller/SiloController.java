package com.grainflow.warehouse.controller;

import com.grainflow.warehouse.dto.ApiResponse;
import com.grainflow.warehouse.dto.silo.*;
import com.grainflow.warehouse.security.AuthenticatedUser;
import com.grainflow.warehouse.service.SiloService;
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
@RequestMapping("/api/v1/silos")
@RequiredArgsConstructor
public class SiloController {

    private final SiloService siloService;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<SiloResponse>> create(
            @Valid @RequestBody CreateSiloRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        SiloResponse response = siloService.create(request, user.companyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Silo created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<SiloResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSiloRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(siloService.update(id, request, user.companyId()), "Silo updated");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        siloService.delete(id, user.companyId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ApiResponse<SiloResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(siloService.getById(id, user.companyId()));
    }

    @GetMapping
    public ApiResponse<Page<SiloResponse>> getAll(
            @ModelAttribute SiloFilterRequest filter,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(siloService.getAll(user.companyId(), filter, pageable));
    }

    @PatchMapping("/{id}/add-grain")
    public ApiResponse<SiloResponse> addGrain(
            @PathVariable UUID id,
            @Valid @RequestBody AddGrainRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(siloService.addGrain(id, request, user.companyId()), "Grain added to silo");
    }

    @PatchMapping("/{id}/remove-grain")
    public ApiResponse<SiloResponse> removeGrain(
            @PathVariable UUID id,
            @Valid @RequestBody RemoveGrainRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(siloService.removeGrain(id, request, user.companyId()), "Grain removed from silo");
    }
}
