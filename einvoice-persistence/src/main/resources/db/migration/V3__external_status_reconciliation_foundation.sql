CREATE TABLE IF NOT EXISTS transmission_external_event (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    tenant_id UUID,
    transmission_id UUID NOT NULL,
    document_id UUID,
    source_type STRING NOT NULL,
    external_status_code STRING,
    external_status_label STRING,
    external_reference STRING,
    deduplication_key STRING NOT NULL,
    raw_payload JSONB,
    occurred_at_external TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    reconciliation_outcome STRING,
    duplicate BOOL NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    metadata JSONB,
    CONSTRAINT uq_transmission_external_event_service_dedup UNIQUE (service_id, deduplication_key)
);

CREATE INDEX IF NOT EXISTS idx_external_event_unprocessed
    ON transmission_external_event (service_id, processed_at, received_at);

CREATE INDEX IF NOT EXISTS idx_external_event_transmission
    ON transmission_external_event (transmission_id, received_at DESC);

ALTER TABLE transmission_record
    ADD COLUMN IF NOT EXISTS last_external_status_code STRING,
    ADD COLUMN IF NOT EXISTS last_external_status_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reconciliation_version INT8 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_status_check_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS status_last_checked_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_transmission_status_refresh
    ON transmission_record (service_id, status, next_status_check_at, updated_at);
