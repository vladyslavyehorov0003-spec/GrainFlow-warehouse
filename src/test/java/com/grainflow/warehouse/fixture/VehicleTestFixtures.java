package com.grainflow.warehouse.fixture;

import com.grainflow.warehouse.dto.vehicle.VehicleResponse;
import com.grainflow.warehouse.entity.CultureType;
import com.grainflow.warehouse.entity.VehicleStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.grainflow.warehouse.fixture.BatchTestFixtures.*;

public class VehicleTestFixtures {

    public static final UUID VEHICLE_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    public static VehicleResponse arrivedVehicle() {
        return new VehicleResponse(
                VEHICLE_ID, COMPANY_A_ID, BATCH_ID,
                "AA1234BB", "Ivan Petrenko",
                CultureType.WHEAT, new BigDecimal("25.500"),
                VehicleStatus.ARRIVED,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                null, null, null,
                null,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                LocalDateTime.of(2026, 4, 14, 8, 0)
        );
    }

    public static VehicleResponse inProcessVehicle() {
        return new VehicleResponse(
                VEHICLE_ID, COMPANY_A_ID, BATCH_ID,
                "AA1234BB", "Ivan Petrenko",
                CultureType.WHEAT, new BigDecimal("25.500"),
                VehicleStatus.IN_PROCESS,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                LocalDateTime.of(2026, 4, 14, 9, 0),
                null, null,
                null,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                LocalDateTime.of(2026, 4, 14, 9, 0)
        );
    }

    public static VehicleResponse acceptedVehicle() {
        return new VehicleResponse(
                VEHICLE_ID, COMPANY_A_ID, BATCH_ID,
                "AA1234BB", "Ivan Petrenko",
                CultureType.WHEAT, new BigDecimal("25.500"),
                VehicleStatus.ACCEPTED,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                LocalDateTime.of(2026, 4, 14, 9, 0),
                LocalDateTime.of(2026, 4, 14, 10, 0),
                LocalDateTime.of(2026, 4, 14, 11, 0),
                null,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                LocalDateTime.of(2026, 4, 14, 11, 0)
        );
    }

    public static VehicleResponse rejectedVehicle() {
        return new VehicleResponse(
                VEHICLE_ID, COMPANY_A_ID, BATCH_ID,
                "AA1234BB", "Ivan Petrenko",
                CultureType.WHEAT, new BigDecimal("25.500"),
                VehicleStatus.REJECTED,
                LocalDateTime.of(2026, 4, 14, 8, 0),
                LocalDateTime.of(2026, 4, 14, 9, 0),
                LocalDateTime.of(2026, 4, 14, 10, 0),
                LocalDateTime.of(2026, 4, 14, 11, 0),
                "moisture too high",
                LocalDateTime.of(2026, 4, 14, 8, 0),
                LocalDateTime.of(2026, 4, 14, 11, 0)
        );
    }
}
