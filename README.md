# TaxFlow

TaxFlow è un servizio di e-invoicing dentro un ecosistema più ampio. Non è la fonte autorevole per tenant master data, IAM/OAuth o gestione mTLS.

## Boundary ecosistemico

La piattaforma centrale resta responsabile di:
- tenant registry,
- service registry,
- identità/OAuth,
- trust mTLS e rotazione certificati.

TaxFlow integra quel modello tramite porte applicative (`EcosystemDirectoryService`, security ports) senza duplicare IAM o registry.

## Scoping operativo (semplificato)

In questo step TaxFlow usa **`service_id` come chiave operativa primaria** per:
- routing connector,
- lookup documenti,
- idempotency submission,
- tracking transmission.

Il `tenant_id` viene risolto a partire dal `service_id` tramite directory ecosistemica (proiezione locale opzionale).

## Moduli

- `einvoice-common`: value object condivisi (`TenantId`, `ServiceId`, riferimenti ecosistema).
- `einvoice-domain`: modello + port di repository.
- `einvoice-application`: orchestrazione submit e porte di integrazione.
- `einvoice-persistence`: schema SQL CockroachDB-ready e adapter repository iniziali in-memory.

## Schema persistence introdotto

Migrazione: `einvoice-persistence/src/main/resources/db/migration/V1__initial_taxflow_schema.sql`.

Tabelle core:
- `fiscal_document`
- `transmission_record`
- `document_artifact`
- `outbox_event`

Tabelle proiezione locale directory:
- `ecosystem_tenant_ref`
- `ecosystem_service_ref`

### Scelte CockroachDB-oriented
- PK UUID ovunque.
- Niente serial/bigserial.
- `version` per optimistic concurrency su `fiscal_document`.
- Vincolo idempotenza su `transmission_record(service_id, document_id, submission_idempotency_key)`.
- Campi canonici in colonna + JSONB per estensioni.

## Deferred

- Adapter JDBC/Cockroach reale (al posto in-memory).
- Dispatcher outbox.
- Client remoto directory ecosistema con caching.
- Object storage reale per payload artifact.
