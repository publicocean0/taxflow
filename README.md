# TaxFlow

TaxFlow è un servizio di e-invoicing dentro un ecosistema più ampio. Non è la fonte autorevole per tenant master data, IAM/OAuth o gestione mTLS.

## Boundary ecosistemico

La piattaforma centrale resta responsabile di:
- tenant registry,
- service registry,
- identità/OAuth,
- trust mTLS e rotazione certificati.

TaxFlow integra quel modello tramite porte applicative (`EcosystemDirectoryService`, security ports) senza duplicare IAM o registry.

## Scoping operativo

TaxFlow usa **`service_id` come chiave operativa primaria** per:
- routing connector,
- lookup documenti,
- idempotency submission,
- tracking transmission.

Il `tenant_id` resta dato derivato dal service directory.

## Reliability model (intent + async execution)

Il submit ora è separato in due fasi:

1. **Submit intent (sincrona e breve)**
   - valida richiesta e documento,
   - crea `transmission_record` in `PENDING_SUBMISSION`,
   - scrive evento `SubmissionRequested` in `outbox_event`,
   - ritorna subito un riferimento trasmissione.

2. **Execution async (fuori transazione originaria)**
   - worker/processor legge outbox,
   - prova claim con transizione stato outbox,
   - esegue `connector.submit` fuori da transazioni DB lunghe,
   - aggiorna stato trasmissione e outbox.

Questo evita side-effect remoti dentro la transazione iniziale e rende il modello più sicuro per CockroachDB e multi-node at-least-once processing.

## Transmission lifecycle

`TransmissionStatus` (foundation):
- `PENDING_SUBMISSION`
- `SUBMITTING`
- `SUBMITTED`
- `STATUS_PENDING`
- `ACCEPTED`
- `REJECTED`
- `FAILED_RETRYABLE`
- `FAILED_FINAL`
- `CANCELLED`

La lifecycle trasmissione è separata dagli stati esterni country/connector. Le integrazioni traducono stato esterno verso stato interno tramite una fase di mapping successiva.

## Outbox pattern foundation

`outbox_event` gestisce:
- `status` (`PENDING`, `PROCESSING`, `PROCESSED`),
- `processing_attempts`,
- `processing_started_at`, `processed_at`,
- `next_attempt_at`, `last_error`.

Il processor è idempotente e multi-node safe su base DB-state:
- claim eventi via transizione stato,
- tolleranza duplicati,
- retry su failure retryable.

## Status update foundation (polling/webhook-ready)

È introdotto `transmission_status_update` per acquisire eventi esterni normalizzati:
- sorgente (`source`),
- codice esterno,
- stato interno mappato,
- payload raw,
- metadata.

Questo abilita futuri flussi polling/webhook senza accoppiare il core a uno specifico provider.

## Schema persistence

Migrazioni:
- `V1__initial_taxflow_schema.sql`
- `V2__transmission_outbox_reliability_foundation.sql`

Tabelle core:
- `fiscal_document`
- `transmission_record`
- `document_artifact`
- `outbox_event`
- `transmission_status_update`

## Deferred

- adapter JDBC/Cockroach reale (al posto in-memory),
- scheduler distribuito/cron robusto,
- broker reale (Kafka/RabbitMQ) opzionale,
- webhook receiver HTTP e signature verification,
- regole di reconciliation connector-specific.
