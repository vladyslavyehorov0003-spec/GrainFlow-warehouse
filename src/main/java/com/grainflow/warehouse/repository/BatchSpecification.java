package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.dto.batch.BatchFilterRequest;
import com.grainflow.warehouse.entity.Batch;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class BatchSpecification {

    public static Specification<Batch> filter(UUID companyId, BatchFilterRequest f) {
        return Specification
                .where(hasCompany(companyId))
                .and(contractNumberContains(f.contractNumber()))
                .and(hasCulture(f))
                .and(hasStatus(f))
                .and(loadingFromAfter(f))
                .and(loadingToBefore(f));
    }

    private static Specification<Batch> hasCompany(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    private static Specification<Batch> contractNumberContains(String contractNumber) {
        return (root, query, cb) -> contractNumber == null ? null
                : cb.like(cb.lower(root.get("contractNumber")), "%" + contractNumber.toLowerCase() + "%");
    }

    private static Specification<Batch> hasCulture(BatchFilterRequest f) {
        return (root, query, cb) -> f.culture() == null ? null
                : cb.equal(root.get("culture"), f.culture());
    }

    private static Specification<Batch> hasStatus(BatchFilterRequest f) {
        return (root, query, cb) -> f.status() == null ? null
                : cb.equal(root.get("status"), f.status());
    }

    private static Specification<Batch> loadingFromAfter(BatchFilterRequest f) {
        return (root, query, cb) -> f.loadingFrom() == null ? null
                : cb.greaterThanOrEqualTo(root.get("loadingFrom"), f.loadingFrom());
    }

    private static Specification<Batch> loadingToBefore(BatchFilterRequest f) {
        return (root, query, cb) -> f.loadingTo() == null ? null
                : cb.lessThanOrEqualTo(root.get("loadingTo"), f.loadingTo());
    }
}
