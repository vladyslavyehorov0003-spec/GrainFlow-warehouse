package com.grainflow.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lab_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    // Each vehicle gets exactly one lab analysis
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false, unique = true)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LabStatus status;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus;

    // --- Analysis stage ---
    @Column
    private LocalDateTime analysisStartedAt;

    @Column
    private LocalDateTime analysisFinishedAt;

    // Moisture content in percent before drying (e.g. 18.50)
    @Column(precision = 5, scale = 2)
    private BigDecimal moisture;

    // Impurity / contamination in percent (e.g. 2.30)
    @Column(precision = 5, scale = 2)
    private BigDecimal impurity;

    // Protein content in percent — relevant for wheat, soybean etc.
    @Column(precision = 5, scale = 2)
    private BigDecimal protein;

    // --- Drying stage ---
    @Column
    private LocalDateTime dryingStartedAt;

    @Column
    private LocalDateTime dryingFinishedAt;

    @Column
    private LocalDateTime estimatedDryingEndAt;

    // Volume loaded onto the dryer, in tonnes
    @Column(precision = 12, scale = 3)
    private BigDecimal volumeBeforeDrying;

    // Volume after drying (less water weight), in tonnes
    @Column(precision = 12, scale = 3)
    private BigDecimal volumeAfterDrying;

    // Moisture after drying in percent
    @Column(precision = 5, scale = 2)
    private BigDecimal moistureAfterDrying;

    // --- Final result ---
    // Actual accepted volume after all processing, in tonnes
    @Column(precision = 12, scale = 3)
    private BigDecimal actualVolume;

    // When PASSED or FAILED decision was recorded
    @Column
    private LocalDateTime decidedAt;

    // Set when grain is moved to a silo (status → STORED)
    @Column
    private UUID siloId;

    @Column
    private LocalDateTime storedAt;

    @Column
    private String comment;


    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = LabStatus.PENDING;
        if (approvalStatus == null) approvalStatus = ApprovalStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
