package com.acme.einvoice.application.service;

import com.acme.einvoice.application.artifact.InMemoryArtifactStorage;
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
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.connectors.spi.ConnectorId;
import com.acme.einvoice.connectors.spi.SubmissionCommand;
import com.acme.einvoice.connectors.spi.SubmissionConnector;
import com.acme.einvoice.connectors.spi.SubmissionResult;
import com.acme.einvoice.country.it.ItalyCountryModule;
import com.acme.einvoice.country.spi.CountryContext;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.DocumentRenderer;
import com.acme.einvoice.country.spi.RenderedDocument;
import com.acme.einvoice.country.spi.StatusTranslator;
import com.acme.einvoice.country.spi.SubmissionPolicy;
import com.acme.einvoice.country.spi.ValidationMessage;
import com.acme.einvoice.country.spi.ValidationReport;
import com.acme.einvoice.domain.model.DocumentLine;
import com.acme.einvoice.domain.model.FiscalDocument;
import com.acme.einvoice.domain.model.Party;
import com.acme.einvoice.domain.model.TaxCategory;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmitDocumentServiceTest {

    @Test
    void submit_happyPath_routesByTenantAndCountry() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        CountryCode italy = CountryCode.of("IT");
        TestFiscalDocument document = new TestFiscalDocument("doc-1", tenantId, italy);

        RecordingConnector connector = new RecordingConnector(ConnectorId.of("SDI_DIRECT"));
        SubmitDocumentService service = service(document, tenantConfig(tenantId, italy, connector.id()), connector, ValidationReport.valid());

        SubmitDocumentResult result = service.execute(new SubmitDocumentCommand(tenantId, document.id()));

        assertEquals("doc-1", result.documentId());
        assertEquals("SUBMITTED", result.outcome());
        assertEquals(connector.id(), connector.lastCommandConnectorId());
        assertNotNull(result.renderedArtifact().orElseThrow().id());
    }

    @Test
    void submit_validationFailure_shortCircuitsBeforeConnectorCall() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        CountryCode italy = CountryCode.of("IT");
        TestFiscalDocument document = new TestFiscalDocument("doc-2", tenantId, italy);

        RecordingConnector connector = new RecordingConnector(ConnectorId.of("SDI_DIRECT"));
        ValidationReport invalid = ValidationReport.of(List.of(
                ValidationMessage.error("INV_001", "missing buyer vat", "buyer.vatNumber")
        ));

        SubmitDocumentService service = service(document, tenantConfig(tenantId, italy, connector.id()), connector, invalid);

        assertThrows(ValidationFailedException.class,
                () -> service.execute(new SubmitDocumentCommand(tenantId, document.id())));
        assertEquals(0, connector.calls());
    }

    @Test
    void submit_failsWhenTenantCountryRoutingMissing() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        CountryCode italy = CountryCode.of("IT");
        TestFiscalDocument document = new TestFiscalDocument("doc-3", tenantId, italy);

        RecordingConnector connector = new RecordingConnector(ConnectorId.of("SDI_DIRECT"));
        TenantFiscalConfiguration disabledCountryConfig = new TenantFiscalConfiguration(
                tenantId,
                Set.of(),
                Map.of(),
                EnvironmentProfile.TEST,
                new SignaturePolicy(false),
                new ArchivePolicy(false)
        );

        SubmitDocumentService service = service(document, disabledCountryConfig, connector, ValidationReport.valid());

        assertThrows(TenantConfigurationNotFoundException.class,
                () -> service.execute(new SubmitDocumentCommand(tenantId, document.id())));
    }

    @Test
    void routing_resolvesConnectorPerTenantCountryBinding() {
        TenantId tenantId = new TenantId(UUID.randomUUID());
        CountryCode italy = CountryCode.of("IT");
        ConnectorId connectorId = ConnectorId.of("SDI_DIRECT");
        RecordingConnector connector = new RecordingConnector(connectorId);

        DefaultRoutingService routingService = new DefaultRoutingService(
                new InMemoryCountryModuleRegistry(List.of(countryModule(ValidationReport.valid()))),
                new InMemoryConnectorRegistry(List.of(connector)),
                new InMemoryTenantConfigurationService(Map.of(tenantId, tenantConfig(tenantId, italy, connectorId)))
        );

        assertEquals(connector, routingService.resolveConnector(tenantId, italy));
    }

    private static SubmitDocumentService service(
            FiscalDocument document,
            TenantFiscalConfiguration configuration,
            RecordingConnector connector,
            ValidationReport report
    ) {
        FiscalDocumentRepository repository = new InMemoryRepository(document);
        CountryModule countryModule = countryModule(report);
        DefaultRoutingService routingService = new DefaultRoutingService(
                new InMemoryCountryModuleRegistry(List.of(countryModule)),
                new InMemoryConnectorRegistry(List.of(connector)),
                new InMemoryTenantConfigurationService(Map.of(document.tenantId(), configuration))
        );

        return new SubmitDocumentService(repository, routingService, new InMemoryArtifactStorage());
    }

    private static CountryModule countryModule(ValidationReport report) {
        return new ItalyCountryModule(
                (document, context) -> report,
                (document, context) -> new RenderedDocument("application/xml", "<xml/>".getBytes(), Map.of("v", "1")),
                new NoopSubmissionPolicy(),
                new NoopStatusTranslator()
        );
    }

    private static TenantFiscalConfiguration tenantConfig(TenantId tenantId, CountryCode country, ConnectorId connectorId) {
        return new TenantFiscalConfiguration(
                tenantId,
                Set.of(country),
                Map.of(country, new ConnectorBinding(country, connectorId)),
                EnvironmentProfile.TEST,
                new SignaturePolicy(false),
                new ArchivePolicy(false)
        );
    }

    private record InMemoryRepository(FiscalDocument document) implements FiscalDocumentRepository {
        @Override public FiscalDocument save(FiscalDocument document) { return document; }
        @Override public Optional<FiscalDocument> findById(String documentId) {
            return document.id().equals(documentId) ? Optional.of(document) : Optional.empty();
        }
        @Override public List<FiscalDocument> findByTenantId(TenantId tenantId) { return List.of(document); }
    }

    private static final class RecordingConnector implements SubmissionConnector {
        private final ConnectorId id;
        private int calls;
        private SubmissionCommand lastCommand;

        private RecordingConnector(ConnectorId id) { this.id = id; }

        @Override public ConnectorId id() { return id; }
        @Override public String type() { return "TEST"; }
        @Override public SubmissionResult submit(SubmissionCommand command) {
            calls++;
            lastCommand = command;
            return new SubmissionResult("tx-1", "SUBMITTED", Optional.of("EXT-1"), Instant.now());
        }
        int calls() { return calls; }
        ConnectorId lastCommandConnectorId() { return id; }
    }

    private record TestFiscalDocument(String id, TenantId tenantId, CountryCode countryCode) implements FiscalDocument {
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
}
