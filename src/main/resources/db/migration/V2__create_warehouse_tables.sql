CREATE TABLE silos
(
    id             UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id     UUID           NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    comment        TEXT,
    max_amount     NUMERIC(12, 3) NOT NULL,
    current_amount NUMERIC(12, 3) NOT NULL,
    culture        VARCHAR(50)    NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE batches
(
    id              UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id      UUID           NOT NULL,
    contract_number VARCHAR(255)   NOT NULL UNIQUE,
    culture         VARCHAR(50)    NOT NULL,
    status          VARCHAR(50)    NOT NULL,
    total_volume    NUMERIC(12, 3) NOT NULL,
    accepted_volume NUMERIC(12, 3) NOT NULL DEFAULT 0,
    unloaded_volume NUMERIC(12, 3) NOT NULL DEFAULT 0,
    loading_from    DATE           NOT NULL,
    loading_to      DATE           NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE vehicles
(
    id                    UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id            UUID           NOT NULL,
    batch_id              UUID           NOT NULL REFERENCES batches (id),
    license_plate         VARCHAR(50)    NOT NULL,
    driver_name           VARCHAR(255),
    culture               VARCHAR(50)    NOT NULL,
    declared_volume       NUMERIC(12, 3) NOT NULL,
    status                VARCHAR(50)    NOT NULL,
    arrived_at            TIMESTAMP      NOT NULL,
    unloading_started_at  TIMESTAMP,
    unloading_finished_at TIMESTAMP,
    decided_at            TIMESTAMP,
    comment               TEXT,
    created_at            TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at            TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE TABLE lab_analyses
(
    id                      UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    company_id              UUID           NOT NULL,
    vehicle_id              UUID           NOT NULL UNIQUE REFERENCES vehicles (id),
    status                  VARCHAR(50)    NOT NULL,
    approval_status         VARCHAR(50)    NOT NULL,
    analysis_started_at     TIMESTAMP,
    analysis_finished_at    TIMESTAMP,
    moisture                NUMERIC(5, 2),
    impurity                NUMERIC(5, 2),
    protein                 NUMERIC(5, 2),
    drying_started_at       TIMESTAMP,
    drying_finished_at      TIMESTAMP,
    estimated_drying_end_at TIMESTAMP,
    volume_before_drying    NUMERIC(12, 3),
    volume_after_drying     NUMERIC(12, 3),
    moisture_after_drying   NUMERIC(5, 2),
    actual_volume           NUMERIC(12, 3),
    decided_at              TIMESTAMP,
    silo_id                 UUID,
    stored_at               TIMESTAMP,
    comment                 TEXT,
    created_at              TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP      NOT NULL DEFAULT now()
);

CREATE INDEX idx_silos_company_id      ON silos (company_id);
CREATE INDEX idx_batches_company_id    ON batches (company_id);
CREATE INDEX idx_vehicles_company_id   ON vehicles (company_id);
CREATE INDEX idx_vehicles_batch_id     ON vehicles (batch_id);
CREATE INDEX idx_lab_analyses_company  ON lab_analyses (company_id);
CREATE INDEX idx_lab_analyses_vehicle  ON lab_analyses (vehicle_id);
