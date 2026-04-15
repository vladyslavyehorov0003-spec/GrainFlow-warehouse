CREATE TABLE audit_log (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    company_id  UUID        NOT NULL,
    action      VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id   UUID,
    changes     JSONB,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_company    ON audit_log (company_id);
CREATE INDEX idx_audit_entity     ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_created_at ON audit_log (created_at DESC);
