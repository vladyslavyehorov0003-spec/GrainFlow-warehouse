package com.grainflow.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    // Contract number agreed with the supplier
    @Column(nullable = false, unique = true)
    private String contractNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CultureType culture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    // Expected total volume under this contract, in tonnes
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal totalVolume;

    // Volume actually accepted into silos so far, in tonnes
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal acceptedVolume;

    // Volume returned to supplier (sold back / unloaded), in tonnes
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal unloadedVolume;

    // Loading window: supplier may dispatch vehicles within this range
    @Column(nullable = false)
    private LocalDate loadingFrom;

    @Column(nullable = false)
    private LocalDate loadingTo;

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
        if (status == null) status = BatchStatus.PLANNED;
        if (acceptedVolume == null) acceptedVolume = BigDecimal.ZERO;
        if (unloadedVolume == null) unloadedVolume = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
