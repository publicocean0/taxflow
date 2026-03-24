package com.acme.einvoice.application.service;

import com.acme.einvoice.application.artifact.InMemoryArtifactStorage;
import com.acme.einvoice.application.ecosystem.InMemoryEcosystemDirectoryService;
import com.acme.einvoice.application.exception.TenantConfigurationNotFoundException;
import com.acme.einvoice.application.exception.ValidationFailedException;
import com.acme.einvoice.application.routing.DefaultRoutingService;
import com.acme.einvoice.application.routing.InMemoryConnectorRegistry;
import com.acme.einvoice.application.routing.InMemoryCountryModuleRegistry;
import com.acme.einvoice.application.tenant.ArchivePolicy;
import com.acme.einvoice.application.tenant.ConnectorBinding;
import com.acme.einvoice.application.tenant.EnvironmentProfile;
import com.acme.einvoice.application.tenant.InMemoryTenantConfigurationService;
import com.acme.einvoice.application.tenant.SignaturePolicy;
import com.acme.einvoice.application.tenant.TenantFiscalConfiguration;
import com.acme.einvoice.application.usecase.SubmitDocumentCommand;
import com.acme.einvoice.application.usecase.SubmitDocumentResult;
import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.EcosystemServiceReference;
import com.acme.einvoice.common.model.EcosystemTenantReference;
import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.connectors.spi.ConnectorId;
import com.acme.einvoice.connectors.spi.SubmissionCommand;
import com.acme.einvoice.connectors.spi.SubmissionConnector;
import com.acme.einvoice.connectors.spi.SubmissionResult;
import com.acme.einvoice.country.it.ItalyCountryModule;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.RenderedDocument;
import com.acme.einvoice.country.spi.StatusTranslator;
import com.acme.einvoice.country.spi.SubmissionPolicy;
import com.acme.einvoice.country.spi.ValidationMessage;
import com.acme.einvoice.country.spi.ValidationReport;
import com.acme.einvoice.domain.model.DocumentLine;
import com.acme.einvoice.domain.model.FiscalDocument;
import com.acme.einvoice.domain.model.OutboxEvent;
import com.acme.einvoice.domain.model.OutboxEventStatus;
import com.acme.einvoice.domain.model.Party;
import com.acme.einvoice.domain.model.TaxCategory;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;
import com.acme.einvoice.persistence.repository.InMemoryOutboxEventRepository;
import com.acme.einvoice.persistence.repository.InMemorySubmissionIntentRepository;
import com.acme.einvoice.persistence.repository.InMemoryTransmissionRepository;
import com.acme.einvoice.persistence.repository.InMemoryTransmissionStatusUpdateRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitDocumentServiceTest {

    @Test
    void submit_intentCreatesPendingTransmissionAndOutbox_andReplayIsIdempotent() {
        Fixture fixture = fixture(ValidationReport.valid(), false);

        SubmitDocumentResult first = fixture.submitService.execute(new SubmitDocumentCommand(fixture.serviceId, fixture.document.id()));
        SubmitDocumentResult replay = fixture.submitService.execute(new SubmitDocumentCommand(fixture.serviceId, fixture.document.id()));

        assertEquals("PENDING_SUBMISSION", first.outcome());
        assertNotNull(first.renderedArtifact().orElseThrow().id());
        assertTrue(replay.idempotentReplay());

        TransmissionRecord transmission = fixture.transmissionRepository.findById(first.transmissionId()).orElseThrow();
        assertEquals(TransmissionStatus.PENDING_SUBMISSION, transmission.status());

        OutboxEvent outbox = fixture.outboxRepository.findProcessable(Instant.now(), 10).getFirst();
        assertEquals(OutboxEventStatus.PENDING, outbox.status());
    }

    @Test
    void asyncProcessor_submitsAndMarksOutboxProcessed_andIsSafeOnReprocessing() {
        Fixture fixture = fixture(ValidationReport.valid(), false);
        SubmitDocumentResult intent = fixture.submitService.execute(new SubmitDocumentCommand(fixture.serviceId, fixture.document.id()));
        String eventId = fixture.outboxRepository.findProcessable(Instant.now(), 10).getFirst().id();

        int processed = fixture.processor.processPendingSubmissions(10);
        int replayProcessed = fixture.processor.processPendingSubmissions(10);

        assertEquals(1, processed);
        assertEquals(0, replayProcessed);
        assertEquals(1, fixture.connector.calls());

        TransmissionRecord transmission = fixture.transmissionRepository.findById(intent.transmissionId()).orElseThrow();
        assertEquals(TransmissionStatus.SUBMITTED, transmission.status());

        OutboxEvent outbox = fixture.outboxRepository.findById(eventId).orElseThrow();
        assertEquals(OutboxEventStatus.PROCESSED, outbox.status());
    }

    @Test
    void asyncProcessor_retryableFailure_movesTransmissionToRetryable() {
        Fixture fixture = fixture(ValidationReport.valid(), true);
        SubmitDocumentResult intent = fixture.submitService.execute(new SubmitDocumentCommand(fixture.serviceId, fixture.document.id()));
        String eventId = fixture.outboxRepository.findProcessable(Instant.now(), 10).getFirst().id();

        fixture.processor.processPendingSubmissions(10);

        TransmissionRecord transmission = fixture.transmissionRepository.findById(intent.transmissionId()).orElseThrow();
        assertEquals(TransmissionStatus.FAILED_RETRYABLE, transmission.status());
        OutboxEvent outbox = fixture.outboxRepository.findById(eventId).orElseThrow();
        assertEquals(OutboxEventStatus.PENDING, outbox.status());
    }

    @Test
    void submit_validationFailure_shortCircuitsBeforeConnectorCall() {
        ValidationReport invalid = ValidationReport.of(List.of(
                ValidationMessage.error("INV_001", "missing buyer vat", "buyer.vatNumber")
        ));
        Fixture fixture = fixture(invalid, false);

        assertThrows(ValidationFailedException.class,
                () -> fixture.submitService.execute(new SubmitDocumentCommand(fixture.serviceId, fixture.document.id())));
        assertEquals(0, fixture.connector.calls());
    }

    @Test
    void routing_resolvesConnectorPerServiceCountryBinding() {
        TenantId tenantId = TenantId.random();
        ServiceId serviceId = ServiceId.random();
        CountryCode italy = CountryCode.of("IT");
        ConnectorId connectorId = ConnectorId.of("SDI_DIRECT");
        RecordingConnector connector = new RecordingConnector(connectorId, false);

        DefaultRoutingService routingService = new DefaultRoutingService(
                new InMemoryCountryModuleRegistry(List.of(countryModule(ValidationReport.valid()))),
                new InMemoryConnectorRegistry(List.of(connector)),
                new InMemoryTenantConfigurationService(Map.of(serviceId, tenantConfig(tenantId, serviceId, italy, connectorId)))
        );

        assertEquals(connector, routingService.resolveConnector(serviceId, italy));
        assertThrows(TenantConfigurationNotFoundException.class,
                () -> routingService.resolveConnector(ServiceId.random(), italy));
    }

    private static Fixture fixture(ValidationReport report, boolean connectorFailure) {
        TenantId tenantId = TenantId.random();
        ServiceId serviceId = ServiceId.random();
        CountryCode italy = CountryCode.of("IT");
        TestFiscalDocument document = new TestFiscalDocument("3fa85f64-5717-4562-b3fc-2c963f66afa1", serviceId, italy, 0);
        RecordingConnector connector = new RecordingConnector(ConnectorId.of("SDI_DIRECT"), connectorFailure);

        TestFiscalDocumentRepository repository = new TestFiscalDocumentRepository();
        repository.save(document);
        CountryModule countryModule = countryModule(report);
        DefaultRoutingService routingService = new DefaultRoutingService(
                new InMemoryCountryModuleRegistry(List.of(countryModule)),
                new InMemoryConnectorRegistry(List.of(connector)),
                new InMemoryTenantConfigurationService(Map.of(serviceId, tenantConfig(tenantId, serviceId, italy, connector.id())))
        );

        InMemoryTransmissionRepository transmissionRepository = new InMemoryTransmissionRepository();
        InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();

        SubmitDocumentService submitService = new SubmitDocumentService(
                repository,
                transmissionRepository,
                new InMemorySubmissionIntentRepository(transmissionRepository, outboxEventRepository),
                routingService,
                new InMemoryArtifactStorage(),
                new InMemoryEcosystemDirectoryService(
                        Map.of(tenantId, new EcosystemTenantReference(tenantId, "Tenant")),
                        Map.of(tenantId, List.of(new EcosystemServiceReference(tenantId, serviceId, "Svc")))
                )
        );

        SubmissionExecutionProcessor processor = new SubmissionExecutionProcessor(
                outboxEventRepository,
                transmissionRepository,
                new InMemoryTransmissionStatusUpdateRepository(),
                repository,
                routingService
        );

        return new Fixture(serviceId, document, connector, submitService, processor, transmissionRepository, outboxEventRepository);
    }

    private static CountryModule countryModule(ValidationReport report) {
        return new ItalyCountryModule(
                (document, context) -> report,
                (document, context) -> new RenderedDocument("application/xml", "<xml/>".getBytes(), Map.of("v", "1")),
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
            TestFiscalDocument document,
            RecordingConnector connector,
            SubmitDocumentService submitService,
            SubmissionExecutionProcessor processor,
            InMemoryTransmissionRepository transmissionRepository,
            InMemoryOutboxEventRepository outboxRepository
    ) {}

    private static final class RecordingConnector implements SubmissionConnector {
        private final ConnectorId id;
        private final boolean shouldFail;
        private int calls;

        private RecordingConnector(ConnectorId id, boolean shouldFail) {
            this.id = id;
            this.shouldFail = shouldFail;
        }

        @Override public ConnectorId id() { return id; }
        @Override public String type() { return "TEST"; }
        @Override public SubmissionResult submit(SubmissionCommand command) {
            calls++;
            if (shouldFail) {
                throw new RuntimeException("temporary connector outage");
            }
            return new SubmissionResult("3fa85f64-5717-4562-b3fc-2c963f66afa9", "SUBMITTED", Optional.of("EXT-1"), Instant.now());
        }
        int calls() { return calls; }
    }

    private record TestFiscalDocument(String id, ServiceId serviceId, CountryCode countryCode, long version) implements FiscalDocument {
        @Override public String documentType() { return "INVOICE"; }
        @Override public Party seller() { return new Party() {
            @Override public String id() { return "s"; }
            @Override public String role() { return "SELLER"; }
            @Override public String legalName() { return "Seller"; }
            @Override public String vatNumber() { return "IT123"; }
        }; }
        @Override public Party buyer() { return seller(); }
        @Override public List<DocumentLine> lines() { return List.of(new DocumentLine() {
            @Override public String id() { return "l1"; }
            @Override public String description() { return "line"; }
            @Override public BigDecimal quantity() { return BigDecimal.ONE; }
            @Override public BigDecimal unitPrice() { return BigDecimal.TEN; }
            @Override public TaxCategory taxCategory() { return new TaxCategory() {
                @Override public String code() { return "VAT"; }
                @Override public BigDecimal rate() { return BigDecimal.valueOf(22); }
                @Override public Optional<String> exemptionReasonCode() { return Optional.empty(); }
            }; }
        }); }
        @Override public Instant issuedAt() { return Instant.now(); }
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

    private static final class TestFiscalDocumentRepository implements FiscalDocumentRepository {
        private final Map<String, FiscalDocument> map = new java.util.concurrent.ConcurrentHashMap<>();

        @Override public FiscalDocument save(FiscalDocument document) { map.put(document.id(), document); return document; }
        @Override public FiscalDocument update(FiscalDocument document, long expectedVersion) { return save(document); }
        @Override public Optional<FiscalDocument> findById(ServiceId serviceId, String documentId) {
            FiscalDocument found = map.get(documentId);
            if (found == null || !found.serviceId().equals(serviceId)) {
                return Optional.empty();
            }
            return Optional.of(found);
        }
        @Override public List<FiscalDocument> findByServiceId(ServiceId serviceId) { return map.values().stream().toList(); }
    }
}
