# TaxFlow

TaxFlow è un servizio di e-invoicing dentro un ecosistema più ampio. Non è la fonte autorevole per tenant master data, IAM/OAuth o gestione mTLS.

## Boundary ecosistemico

La piattaforma centrale resta responsabile di tenant registry, service registry, identità/OAuth e trust mTLS.
TaxFlow integra quel modello tramite porte applicative senza duplicare IAM o registry.

## Scoping operativo

TaxFlow usa **`service_id` come chiave operativa primaria** per routing, lookup documenti, idempotency e tracking transmission.
`tenant_id` resta dato derivato dal service directory.

## Submission vs reconciliation

Il modello è separato in due fasi:

1. **Submission pipeline**
    - submit intent crea `transmission_record` + outbox;
    - async processor esegue `connector.submit`;
    - stato locale passa a `SUBMITTED`/failure.

2. **External status reconciliation pipeline**
    - TaxFlow acquisisce evidenze esterne da polling o webhook;
    - salva prima l’evidenza (`transmission_external_event`);
    - traduce stato esterno in update interno normalizzato;
    - applica transizione idempotente su `transmission_record`;
    - traccia audit in `transmission_status_update`.

## Polling e webhook ingestion model

- **Polling**: `TransmissionStatusPollingService` trova trasmissioni eleggibili (`SUBMITTED`/`STATUS_PENDING` e `next_status_check_at` scaduto), invoca `SubmissionConnector.fetchStatus`, persiste evento esterno deduplicato.
- **Webhook-ready ingestion**: `ConnectorWebhookIngestionService` espone un boundary applicativo interno (`IngestExternalStatusUpdateUseCase`) per ingest di update esterni service-scoped, con dedup e reconciliation immediata.

## External evidence / audit model

Nuova tabella: `transmission_external_event`.
Campi chiave:
- service scope (`service_id`, `transmission_id`),
- `source_type` (`POLLING`, `WEBHOOK`, ...),
- status esterno (`external_status_code`/label/reference),
- `deduplication_key` unico per service,
- payload raw/metadata,
- `processed_at`, `reconciliation_outcome`, `duplicate`.

`transmission_record` include metadata reconciliation:
- `last_external_status_code`, `last_external_status_at`,
- `reconciliation_version`,
- `next_status_check_at`, `status_last_checked_at`.

## Duplicate / out-of-order philosophy

TaxFlow assume at-least-once processing e multi-node concorrente.
Correttezza ottenuta via:
- deduplicazione persistente per `(service_id, deduplication_key)`,
- reconciliation idempotente,
- optimistic concurrency su `status_version`,
- regole anti-regressione (eventi outdated o terminal state non regrediscono).

## Multi-node e CockroachDB implications

- Nessuna coordinazione in-memory tra nodi.
- Più nodi possono processare polling/reconciliation in parallelo.
- Eventi duplicati e retry sono attesi.
- Transazioni corte: evidence prima, poi apply.
- Schema e indici restano service-centric e Cockroach-friendly.

## Schema persistence

Migrazioni:
- `V1__initial_taxflow_schema.sql`
- `V2__transmission_outbox_reliability_foundation.sql`
- `V3__external_status_reconciliation_foundation.sql`

## Deferred

- scheduler distribuito/cron production-grade,
- adapter HTTP webhook + signature verification,
- connector/country reconciliation matrix avanzata,
- repair/ops UI,
- adapter JDBC/Cockroach reale al posto repository in-memory.
