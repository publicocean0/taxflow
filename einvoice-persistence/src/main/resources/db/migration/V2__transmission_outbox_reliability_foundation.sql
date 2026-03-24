ALTER TABLE transmission_record
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS processing_attempts INT8 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_error_code STRING,
    ADD COLUMN IF NOT EXISTS last_error_message STRING,
    ADD COLUMN IF NOT EXISTS status_version INT8 NOT NULL DEFAULT 0;

ALTER TABLE transmission_record
    ALTER COLUMN submitted_at DROP NOT NULL;

CREATE INDEX IF NOT EXISTS idx_transmission_status_retry
    ON transmission_record (status, next_attempt_at, updated_at);

ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS status STRING NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS processing_started_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS processing_attempts INT8 NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS next_attempt_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_error STRING;

CREATE INDEX IF NOT EXISTS idx_outbox_status_next_attempt
    ON outbox_event (status, next_attempt_at, created_at);

CREATE TABLE IF NOT EXISTS transmission_status_update (
    id UUID PRIMARY KEY,
    transmission_id UUID NOT NULL,
    source STRING NOT NULL,
    external_status_code STRING,
    external_reference STRING,
    mapped_status STRING NOT NULL,
    raw_payload JSONB,
    received_at TIMESTAMPTZ NOT NULL,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_status_update_transmission
    ON transmission_status_update (transmission_id, received_at DESC);
