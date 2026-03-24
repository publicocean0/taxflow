CREATE TABLE IF NOT EXISTS fiscal_document (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    country_code STRING NOT NULL,
    document_type STRING NOT NULL,
    status STRING NOT NULL,
    issue_date DATE,
    currency_code STRING,
    document_number STRING,
    seller_reference STRING,
    buyer_reference STRING,
    net_amount DECIMAL(18,2),
    tax_amount DECIMAL(18,2),
    gross_amount DECIMAL(18,2),
    country_extension_type STRING,
    country_extension_version STRING,
    country_data JSONB,
    domain_data JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version INT8 NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_fiscal_document_service_id ON fiscal_document (service_id, id);
CREATE INDEX IF NOT EXISTS idx_fiscal_document_service_status ON fiscal_document (service_id, status);
CREATE INDEX IF NOT EXISTS idx_fiscal_document_service_docno ON fiscal_document (service_id, document_number);

CREATE TABLE IF NOT EXISTS transmission_record (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    document_id UUID NOT NULL,
    connector_id STRING NOT NULL,
    submission_idempotency_key STRING NOT NULL,
    external_reference STRING,
    status STRING NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    metadata JSONB
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_transmission_service_document_idempotency
    ON transmission_record (service_id, document_id, submission_idempotency_key);
CREATE INDEX IF NOT EXISTS idx_transmission_service_document ON transmission_record (service_id, document_id);

CREATE TABLE IF NOT EXISTS document_artifact (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL,
    document_id UUID NOT NULL,
    artifact_type STRING NOT NULL,
    format STRING NOT NULL,
    storage_uri STRING NOT NULL,
    checksum STRING,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_document_artifact_service_document ON document_artifact (service_id, document_id);

CREATE TABLE IF NOT EXISTS outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type STRING NOT NULL,
    aggregate_id UUID NOT NULL,
    service_id UUID,
    event_type STRING NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_outbox_unpublished ON outbox_event (published_at, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_service ON outbox_event (service_id, created_at);

CREATE TABLE IF NOT EXISTS ecosystem_tenant_ref (
    tenant_id UUID PRIMARY KEY,
    display_name STRING,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS ecosystem_service_ref (
    service_id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    service_name STRING,
    updated_at TIMESTAMPTZ NOT NULL
);
