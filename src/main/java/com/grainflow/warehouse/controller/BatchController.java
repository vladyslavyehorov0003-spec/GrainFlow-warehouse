package com.grainflow.warehouse.controller;

import com.grainflow.warehouse.dto.ApiResponse;
import com.grainflow.warehouse.dto.batch.AddVolumeRequest;
import com.grainflow.warehouse.dto.batch.BatchResponse;
import com.grainflow.warehouse.dto.batch.CreateBatchRequest;
import com.grainflow.warehouse.dto.batch.UpdateBatchRequest;
import com.grainflow.warehouse.dto.batch.BatchFilterRequest;
import com.grainflow.warehouse.security.AuthenticatedUser;
import com.grainflow.warehouse.service.BatchService;
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
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    // --- MANAGER only ---

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<BatchResponse>> create(
            @Valid @RequestBody CreateBatchRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        BatchResponse response = batchService.create(request, user.companyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Batch created"));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<BatchResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateBatchRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(batchService.update(id, request, user.companyId()), "Batch updated");
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> close(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        batchService.close(id, user.companyId());
        return ResponseEntity.noContent().build();
    }

    // --- Any authenticated user ---

    @GetMapping("/{id}")
    public ApiResponse<BatchResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(batchService.getById(id, user.companyId()));
    }

    @GetMapping
    public ApiResponse<Page<BatchResponse>> getAll(
            @ModelAttribute BatchFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(batchService.getAll(user.companyId(), filter, pageable));
    }

    @PatchMapping("/{id}/accepted-volume")
    public ApiResponse<BatchResponse> addAcceptedVolume(
            @PathVariable UUID id,
            @Valid @RequestBody AddVolumeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(batchService.addAcceptedVolume(id, request, user.companyId()), "Accepted volume updated");
    }

    @PatchMapping("/{id}/unloaded-volume")
    public ApiResponse<BatchResponse> addUnloadedVolume(
            @PathVariable UUID id,
            @Valid @RequestBody AddVolumeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return ApiResponse.success(batchService.addUnloadedVolume(id, request, user.companyId()), "Unloaded volume updated");
    }
}
