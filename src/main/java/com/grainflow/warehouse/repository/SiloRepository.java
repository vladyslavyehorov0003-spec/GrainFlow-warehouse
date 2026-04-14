package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.entity.Silo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SiloRepository extends JpaRepository<Silo, UUID>, JpaSpecificationExecutor<Silo> {

    boolean existsByNameAndCompanyId(String name, UUID companyId);
}
