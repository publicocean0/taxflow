# taxflow

Skeleton iniziale per una piattaforma e-invoicing multi-country, multi-tenant, disegnata con core astratto + plugin paese + plugin connector.

## Struttura Maven multi-module

- `einvoice-common`: value objects condivisi (es. `CountryCode`, `TenantId`).
- `einvoice-domain`: dominio fiscale universale (`FiscalDocument`, `Invoice`, `DocumentStatus`, repository ports).
- `einvoice-country-spi`: SPI dei plugin paese (`CountryModule`, validator, renderer, status translator) + nuovo modello di validazione ricco.
- `einvoice-connectors-spi`: SPI dei canali esterni (`SubmissionConnector`, `ConnectorId`, `SubmissionCommand`, `SubmissionResult`).
- `einvoice-application`: use case applicativi, routing tenant-aware, configurazione fiscale tenant, artifact ports.
- `einvoice-grpc-contract`: contratti `.proto` gRPC.
- `einvoice-grpc-server`: facade inbound gRPC (adapter sottile verso use case).
- `einvoice-country-it`: primo adapter paese-specifico per Italia (`ItalyCountryModule`).
- `einvoice-connector-sdi`: primo connector tecnico di submission (`SdiSubmissionConnector`).
- `einvoice-bootstrap`: entrypoint bootstrap runtime con wiring in-memory.

## Routing layer tenant-aware

L'orchestrazione di submission usa ora un livello di routing esplicito:

- `RoutingService`: risolve `CountryModule` per `countryCode` e `SubmissionConnector` per `tenant + country`.
- `CountryModuleRegistry`: lookup dei country module disponibili (`get`, `all`).
- `ConnectorRegistry`: lookup dei connector disponibili (`get`, `all`).
- `TenantConfigurationService`: fornisce la configurazione fiscale tenant.

La risoluzione fallisce con eccezioni applicative esplicite (`CountryModuleNotFoundException`, `TenantConfigurationNotFoundException`, `ConnectorNotFoundException`) invece di errori generici.

## Tenant fiscal configuration

È stato introdotto un modello minimo ma estendibile:

- `TenantFiscalConfiguration`
  - `tenantId`
  - `enabledCountries`
  - `connectorBindings` per paese (`ConnectorBinding`)
  - `environmentProfile` (`TEST` / `PRODUCTION`)
  - placeholders evolutivi: `SignaturePolicy`, `ArchivePolicy`

Per bootstrap/demo è presente `InMemoryTenantConfigurationService`.

## Rich validation model

Il contratto `CountryValidator` restituisce ora un `ValidationReport` ricco:

- validità calcolata da severità (`ERROR` invalida, `WARNING` no)
- `ValidationMessage` con:
  - `code` machine-readable
  - `message` human-readable
  - `path` opzionale campo/segmento
  - `severity` (`ERROR`, `WARNING`)

L'`application service` interrompe il flusso con `ValidationFailedException` se il report è invalido.

## Submit orchestration (nuovo step)

`SubmitDocumentService` ora:

1. carica documento da repository,
2. risolve country module tramite `RoutingService`,
3. valida con `ValidationReport`,
4. renderizza,
5. persiste artifact renderizzato tramite `ArtifactStorage`,
6. risolve connector tenant/country tramite `RoutingService`,
7. invia al connector,
8. ritorna `SubmitDocumentResult` strutturato.

## Next planned steps

1. Persistenza reale per tenant configuration e artifact storage.
2. Firma digitale policy-driven (per tenant/country).
3. Tracking asincrono submission status (polling/webhook/outbox).
4. Mapping errori applicativi → status gRPC dedicati.
5. Nuovi country modules e connector adapters oltre Italia/SDI.
