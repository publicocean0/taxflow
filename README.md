# taxflow

Skeleton iniziale per una piattaforma e-invoicing multi-country, multi-tenant, disegnata con core astratto + plugin paese + plugin connector.

## Struttura Maven multi-module

- `einvoice-common`: value objects condivisi (es. `CountryCode`, `TenantId`).
- `einvoice-domain`: dominio fiscale universale (`FiscalDocument`, `Invoice`, `DocumentStatus`, repository ports).
- `einvoice-country-spi`: SPI dei plugin paese (`CountryModule`, validator, renderer, status translator).
- `einvoice-connectors-spi`: SPI dei canali esterni (`SubmissionConnector`, `SubmissionCommand`, `SubmissionResult`).
- `einvoice-application`: use case applicativi (es. `SubmitDocumentService`) e routing ports.
- `einvoice-grpc-contract`: contratti `.proto` gRPC.
- `einvoice-grpc-server`: facade inbound gRPC (adapter sottile verso use case).
- `einvoice-country-it`: primo adapter paese-specifico per Italia (`ItalyCountryModule`).
- `einvoice-connector-sdi`: primo connector tecnico di submission (`SdiSubmissionConnector`).
- `einvoice-bootstrap`: entrypoint bootstrap runtime.

## Principio architetturale

Il core non contiene dettagli FatturaPA/SdI: tratta solo astrazioni universali di documento fiscale, validazione, rendering, submission e tracking.
I dettagli paese-specifici vivono nei plugin paese, i dettagli infrastrutturali nei connector.

## Stato attuale

Questo repository contiene un *project skeleton* con interfacce e classi base, utile come punto di partenza per implementare:

1. workflow documentale completo,
2. persistenza e outbox,
3. firma digitale,
4. polling/webhook stati esterni,
5. binding runtime tenant → country module/connector.
