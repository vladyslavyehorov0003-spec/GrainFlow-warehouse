package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.dto.lab.LabAnalysisFilterRequest;
import com.grainflow.warehouse.entity.LabAnalysis;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class LabAnalysisSpecification {

    public static Specification<LabAnalysis> filter(UUID companyId, LabAnalysisFilterRequest f) {
        return Specification
                .where(hasCompany(companyId))
                .and(hasVehicle(f))
                .and(hasBatch(f))
                .and(hasStatus(f));
    }

    private static Specification<LabAnalysis> hasCompany(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    private static Specification<LabAnalysis> hasVehicle(LabAnalysisFilterRequest f) {
        return (root, query, cb) -> f.vehicleId() == null ? null
                : cb.equal(root.get("vehicle").get("id"), f.vehicleId());
    }

    private static Specification<LabAnalysis> hasBatch(LabAnalysisFilterRequest f) {
        return (root, query, cb) -> f.batchId() == null ? null
                : cb.equal(root.get("vehicle").get("batch").get("id"), f.batchId());
    }

    private static Specification<LabAnalysis> hasStatus(LabAnalysisFilterRequest f) {
        return (root, query, cb) -> f.status() == null ? null
                : cb.equal(root.get("status"), f.status());
    }
}
