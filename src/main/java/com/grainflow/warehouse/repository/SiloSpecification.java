package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.dto.silo.SiloFilterRequest;
import com.grainflow.warehouse.entity.Silo;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class SiloSpecification {

    public static Specification<Silo> filter(UUID companyId, SiloFilterRequest f) {
        return Specification
                .where(hasCompany(companyId))
                .and(nameContains(f))
                .and(hasCulture(f));
    }

    private static Specification<Silo> hasCompany(UUID companyId) {
        return (root, query, cb) -> cb.equal(root.get("companyId"), companyId);
    }

    private static Specification<Silo> nameContains(SiloFilterRequest f) {
        return (root, query, cb) -> f.name() == null ? null
                : cb.like(cb.lower(root.get("name")), "%" + f.name().toLowerCase() + "%");
    }

    private static Specification<Silo> hasCulture(SiloFilterRequest f) {
        return (root, query, cb) -> f.culture() == null ? null
                : cb.equal(root.get("culture"), f.culture());
    }
}
