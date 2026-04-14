package com.grainflow.warehouse.controller;

import com.grainflow.warehouse.dto.response.ApiResponse;
import com.grainflow.warehouse.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Tag(name = "Ping", description = "Auth connectivity check")
public class PingController {

    // Verifies that warehouse-service correctly validates tokens via auth-service.
    // Returns the authenticated user context extracted from the token.
    @GetMapping("/ping")
    @Operation(
            summary = "Ping",
            description = "Returns authenticated user context — use to verify auth-service connectivity",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping(
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        Map<String, Object> data = Map.of(
                "userId",    currentUser.userId(),
                "companyId", currentUser.companyId(),
                "email",     currentUser.email(),
                "role",      currentUser.role()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "Warehouse auth is working"));
    }
}
