package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.dto.vehicle.VehicleFilterRequest;
import com.grainflow.warehouse.entity.Vehicle;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class VehicleSpecification {

    public static Specification<Vehicle> filter(UUID companyId, VehicleFilterRequest f) {
        return Specification
                .where(hasCompany(companyId))
                .and(hasBatch(f))
                .and(hasStatus(f))
                .and(hasCulture(f))
                .and(licensePlateContains(f));
    }

    private static Specification<Vehicle> hasCompany(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    private static Specification<Vehicle> hasBatch(VehicleFilterRequest f) {
        return (root, query, cb) -> f.batchId() == null ? null
                : cb.equal(root.get("batch").get("id"), f.batchId());
    }

    private static Specification<Vehicle> hasStatus(VehicleFilterRequest f) {
        return (root, query, cb) -> f.status() == null ? null
                : cb.equal(root.get("status"), f.status());
    }

    private static Specification<Vehicle> hasCulture(VehicleFilterRequest f) {
        return (root, query, cb) -> f.culture() == null ? null
                : cb.equal(root.get("culture"), f.culture());
    }

    private static Specification<Vehicle> licensePlateContains(VehicleFilterRequest f) {
        return (root, query, cb) -> f.licensePlate() == null ? null
                : cb.like(cb.lower(root.get("licensePlate")), "%" + f.licensePlate().toLowerCase() + "%");
    }
}
