package com.grainflow.warehouse.repository;

import com.grainflow.warehouse.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID>, JpaSpecificationExecutor<Batch> {

    boolean existsByContractNumberAndCompanyId(String contractNumber, UUID companyId);
}
