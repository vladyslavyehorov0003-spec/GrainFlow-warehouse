-- Optimistic locking: prevents concurrent addGrain/removeGrain/addVolume
-- from producing inconsistent amounts.
-- Hibernate reads the version on SELECT and checks it again on UPDATE (WHERE version = N).
-- If another transaction already wrote, the version won't match → 409 to the client.

ALTER TABLE silos   ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE batches ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
