package com.acme.einvoice.application.service;

import com.acme.einvoice.application.routing.DefaultRoutingService;
import com.acme.einvoice.application.routing.InMemoryConnectorRegistry;
import com.acme.einvoice.application.routing.InMemoryCountryModuleRegistry;
import com.acme.einvoice.application.tenant.ArchivePolicy;
import com.acme.einvoice.application.tenant.ConnectorBinding;
import com.acme.einvoice.application.tenant.EnvironmentProfile;
import com.acme.einvoice.application.tenant.InMemoryTenantConfigurationService;
import com.acme.einvoice.application.tenant.SignaturePolicy;
import com.acme.einvoice.application.tenant.TenantFiscalConfiguration;
import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.connectors.spi.ConnectorId;
import com.acme.einvoice.connectors.spi.ExternalStatusResult;
import com.acme.einvoice.connectors.spi.StatusQueryCommand;
import com.acme.einvoice.connectors.spi.SubmissionCommand;
import com.acme.einvoice.connectors.spi.SubmissionConnector;
import com.acme.einvoice.connectors.spi.SubmissionResult;
import com.acme.einvoice.country.it.ItalyCountryModule;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.RenderedDocument;
import com.acme.einvoice.country.spi.StatusTranslator;
import com.acme.einvoice.country.spi.SubmissionPolicy;
import com.acme.einvoice.country.spi.ValidationReport;
import com.acme.einvoice.domain.model.ReconciliationOutcome;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;
import com.acme.einvoice.persistence.repository.InMemoryFiscalDocumentRepository;
import com.acme.einvoice.persistence.repository.InMemoryTransmissionExternalEventRepository;
import com.acme.einvoice.persistence.repository.InMemoryTransmissionRepository;
import com.acme.einvoice.persistence.repository.InMemoryTransmissionStatusUpdateRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalStatusReconciliationTest {

    @Test
    void polling_storesExternalEvent_andReconciliationUpdatesTransmission() {
        Fixture fixture = fixture();
        fixture.connector.statusResult = Optional.of(new ExternalStatusResult(
                "POLLING",
                Optional.of("ACCEPTED"),
                Optional.of("Accepted"),
                Optional.of("EXT-1"),
                "poll-1",
                Optional.of("{\"s\":\"ACCEPTED\"}"),
                Optional.of(Instant.now()),
                Instant.now(),
                Map.of("connector", "TEST")
        ));

        int polled = fixture.pollingService.pollEligibleTransmissions(fixture.serviceId, 10);
        int reconciled = fixture.reconciliationProcessor.reconcilePending(fixture.serviceId, 10);

        assertEquals(1, polled);
        assertEquals(1, reconciled);
        assertEquals(TransmissionStatus.ACCEPTED, fixture.transmissionRepository.findById(fixture.transmission.id()).orElseThrow().status());
    }

    @Test
    void duplicate_externalEvent_isIgnoredSafely() {
        Fixture fixture = fixture();

        ReconciliationOutcome first = fixture.webhookIngestionService.ingest(new IngestExternalStatusUpdateUseCase.IngestExternalStatusUpdateCommand(
                fixture.serviceId,
                fixture.transmission.id(),
                "WEBHOOK",
                "dup-1",
                "PENDING",
                "Pending",
                "EXT-1",
                "{}"
        ));
        ReconciliationOutcome second = fixture.webhookIngestionService.ingest(new IngestExternalStatusUpdateUseCase.IngestExternalStatusUpdateCommand(
                fixture.serviceId,
                fixture.transmission.id(),
                "WEBHOOK",
                "dup-1",
                "PENDING",
                "Pending",
                "EXT-1",
                "{}"
        ));

        assertEquals(ReconciliationOutcome.APPLIED, first);
        assertEquals(ReconciliationOutcome.DUPLICATE, second);
        assertEquals(1, fixture.externalEventRepository.findByTransmissionId(fixture.transmission.id()).size());
    }

    @Test
    void outOfOrder_event_doesNotRegressState() {
        Fixture fixture = fixture();
        ReconciliationOutcome accepted = fixture.webhookIngestionService.ingest(new IngestExternalStatusUpdateUseCase.IngestExternalStatusUpdateCommand(
                fixture.serviceId,
                fixture.transmission.id(),
                "WEBHOOK",
                "ord-1",
                "ACCEPTED",
                "Accepted",
                "EXT-1",
                "{}"
        ));
        ReconciliationOutcome oldPending = fixture.webhookIngestionService.ingest(new IngestExternalStatusUpdateUseCase.IngestExternalStatusUpdateCommand(
                fixture.serviceId,
                fixture.transmission.id(),
                "WEBHOOK",
                "ord-2",
                "PENDING",
                "Pending",
                "EXT-1",
                "{}"
        ));

        assertEquals(ReconciliationOutcome.APPLIED, accepted);
        assertEquals(ReconciliationOutcome.IGNORED_TERMINAL, oldPending);
        assertEquals(TransmissionStatus.ACCEPTED, fixture.transmissionRepository.findById(fixture.transmission.id()).orElseThrow().status());
    }

    @Test
    void reprocessing_onMultipleNodes_isIdempotent() {
        Fixture fixture = fixture();
        fixture.webhookIngestionService.ingest(new IngestExternalStatusUpdateUseCase.IngestExternalStatusUpdateCommand(
                fixture.serviceId,
                fixture.transmission.id(),
                "WEBHOOK",
                "multi-1",
                "PENDING",
                "Pending",
                "EXT-1",
                "{}"
        ));

        int first = fixture.reconciliationProcessor.reconcilePending(fixture.serviceId, 10);
        int second = fixture.reconciliationProcessor.reconcilePending(fixture.serviceId, 10);

        assertEquals(0, first);
        assertEquals(0, second);
        assertTrue(fixture.statusUpdateRepository.findByTransmissionId(fixture.transmission.id()).size() >= 1);
    }

    private static Fixture fixture() {
        TenantId tenantId = TenantId.random();
        ServiceId serviceId = ServiceId.random();
        CountryCode italy = CountryCode.of("IT");

        TestFiscalDocument document = new TestFiscalDocument("3fa85f64-5717-4562-b3fc-2c963f66aff1", serviceId, italy, 0);
        InMemoryFiscalDocumentRepository docRepository = new InMemoryFiscalDocumentRepository();
        docRepository.save(document);

        PollingConnector connector = new PollingConnector();
        DefaultRoutingService routingService = new DefaultRoutingService(
                new InMemoryCountryModuleRegistry(List.of(countryModule())),
                new InMemoryConnectorRegistry(List.of(connector)),
                new InMemoryTenantConfigurationService(Map.of(serviceId, tenantConfig(tenantId, serviceId, italy, connector.id())))
        );

        InMemoryTransmissionRepository transmissionRepository = new InMemoryTransmissionRepository();
        TransmissionRecord transmission = transmissionRepository.save(new TransmissionRecord(
                "c2fb445d-2e4d-4a10-b437-eea2a4d25bc2",
                serviceId,
                document.id(),
                connector.id().value(),
                "idem-1",
                Optional.of("EXT-1"),
                TransmissionStatus.SUBMITTED,
                Optional.of(Instant.now().minusSeconds(60)),
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(60),
                1,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                Optional.empty(),
                Optional.empty(),
                0,
                Optional.of(Instant.now().minusSeconds(1)),
                Optional.empty(),
                Map.of("tenantId", tenantId.value().toString())
        ));

        InMemoryTransmissionExternalEventRepository externalEventRepository = new InMemoryTransmissionExternalEventRepository();
        InMemoryTransmissionStatusUpdateRepository statusUpdateRepository = new InMemoryTransmissionStatusUpdateRepository();
        ReconciliationProcessor reconciliationProcessor = new ReconciliationProcessor(
                externalEventRepository,
                transmissionRepository,
                statusUpdateRepository,
                new DefaultExternalStatusTranslator()
        );

        return new Fixture(
                serviceId,
                transmission,
                connector,
                transmissionRepository,
                externalEventRepository,
                statusUpdateRepository,
                new TransmissionStatusPollingService(transmissionRepository, docRepository, externalEventRepository, routingService),
                reconciliationProcessor,
                new ConnectorWebhookIngestionService(externalEventRepository, reconciliationProcessor)
        );
    }

    private static CountryModule countryModule() {
        return new ItalyCountryModule(
                (document, context) -> ValidationReport.valid(),
                (document, context) -> new RenderedDocument("application/xml", "<xml/>".getBytes(), Map.of()),
                new NoopSubmissionPolicy(),
                new NoopStatusTranslator()
        );
    }

    private static TenantFiscalConfiguration tenantConfig(TenantId tenantId, ServiceId serviceId, CountryCode country, ConnectorId connectorId) {
        return new TenantFiscalConfiguration(
                tenantId,
                serviceId,
                Set.of(country),
                Map.of(country, new ConnectorBinding(country, connectorId)),
                EnvironmentProfile.TEST,
                new SignaturePolicy(false),
                new ArchivePolicy(false)
        );
    }

    private record Fixture(
            ServiceId serviceId,
            TransmissionRecord transmission,
            PollingConnector connector,
            InMemoryTransmissionRepository transmissionRepository,
            InMemoryTransmissionExternalEventRepository externalEventRepository,
            InMemoryTransmissionStatusUpdateRepository statusUpdateRepository,
            TransmissionStatusPollingService pollingService,
            ReconciliationProcessor reconciliationProcessor,
            ConnectorWebhookIngestionService webhookIngestionService
    ) {}

    private static final class PollingConnector implements SubmissionConnector {
        private Optional<ExternalStatusResult> statusResult = Optional.empty();
        @Override public ConnectorId id() { return ConnectorId.of("SDI_DIRECT"); }
        @Override public String type() { return "TEST"; }
        @Override public SubmissionResult submit(SubmissionCommand command) { throw new UnsupportedOperationException(); }
        @Override public Optional<ExternalStatusResult> fetchStatus(StatusQueryCommand command) { return statusResult; }
    }

    private static final class NoopSubmissionPolicy implements SubmissionPolicy {
        @Override public boolean requiresSignature() { return false; }
        @Override public String connectorType() { return "TEST"; }
    }

    private static final class NoopStatusTranslator implements StatusTranslator {
        @Override public com.acme.einvoice.domain.model.DocumentStatus translate(String externalStatus) {
            return com.acme.einvoice.domain.model.DocumentStatus.SUBMITTED;
        }
    }

    private record TestFiscalDocument(String id, ServiceId serviceId, CountryCode countryCode, long version)
            implements com.acme.einvoice.domain.model.FiscalDocument {
        @Override public String documentType() { return "INVOICE"; }
        @Override public com.acme.einvoice.domain.model.Party seller() { return null; }
        @Override public com.acme.einvoice.domain.model.Party buyer() { return null; }
        @Override public List<com.acme.einvoice.domain.model.DocumentLine> lines() { return List.of(); }
        @Override public Instant issuedAt() { return Instant.now(); }
    }
}
