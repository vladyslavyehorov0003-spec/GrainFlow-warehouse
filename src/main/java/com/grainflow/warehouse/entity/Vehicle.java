package com.grainflow.warehouse.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID companyId;

    // Contract this vehicle belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    // Truck licence plate
    @Column(nullable = false)
    private String licensePlate;

    @Column
    private String driverName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CultureType culture;

    // Volume declared by the driver on arrival, in tonnes
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal declaredVolume;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    // --- Status timestamps ---

    // ARRIVED: when the truck physically arrived at the elevator
    @Column(nullable = false)
    private LocalDateTime arrivedAt;

    // IN_PROCESS: when unloading started / finished
    @Column
    private LocalDateTime unloadingStartedAt;

    @Column
    private LocalDateTime unloadingFinishedAt;

    // ACCEPTED or REJECTED: when the final decision was made
    @Column
    private LocalDateTime decidedAt;

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
        if (status == null) status = VehicleStatus.ARRIVED;
        if (arrivedAt == null) arrivedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
