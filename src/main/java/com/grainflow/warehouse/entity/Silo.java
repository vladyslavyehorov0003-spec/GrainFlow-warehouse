package com.grainflow.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "silos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Silo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Optimistic lock — prevents two concurrent addGrain/removeGrain from exceeding capacity.
    // JPA appends "AND version=N" to every UPDATE; if another transaction already wrote,
    // version mismatch → ObjectOptimisticLockingFailureException → 409 to the client.
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    @Column
    private String comment;

    // Maximum storage capacity in tonnes
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal maxAmount;

    // Current amount stored in tonnes
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal currentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CultureType culture;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
