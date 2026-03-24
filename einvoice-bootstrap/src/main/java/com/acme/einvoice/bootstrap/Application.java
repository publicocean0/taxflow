package com.acme.einvoice.bootstrap;

import com.acme.einvoice.application.artifact.InMemoryArtifactStorage;
import com.acme.einvoice.application.routing.DefaultRoutingService;
import com.acme.einvoice.application.routing.InMemoryConnectorRegistry;
import com.acme.einvoice.application.routing.InMemoryCountryModuleRegistry;
import com.acme.einvoice.application.service.SubmitDocumentService;
import com.acme.einvoice.application.tenant.ArchivePolicy;
import com.acme.einvoice.application.tenant.ConnectorBinding;
import com.acme.einvoice.application.tenant.EnvironmentProfile;
import com.acme.einvoice.application.tenant.InMemoryTenantConfigurationService;
import com.acme.einvoice.application.tenant.SignaturePolicy;
import com.acme.einvoice.application.tenant.TenantFiscalConfiguration;
import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.connector.sdi.SdiSubmissionConnector;
import com.acme.einvoice.connectors.spi.ConnectorId;
import com.acme.einvoice.country.it.ItalyCountryModule;
import com.acme.einvoice.country.spi.ValidationReport;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Application {
    private Application() {
    }

    public static void main(String[] args) {
        CountryCode italy = CountryCode.of("IT");
        TenantId tenantId = new TenantId(UUID.fromString("00000000-0000-0000-0000-000000000001"));

        var italyModule = new ItalyCountryModule(
                (document, context) -> ValidationReport.valid(),
                (document, context) -> new com.acme.einvoice.country.spi.RenderedDocument("application/xml", "<xml/>".getBytes(), Map.of()),
                new com.acme.einvoice.country.spi.SubmissionPolicy() {
                    @Override
                    public boolean requiresSignature() {
                        return false;
                    }

                    @Override
                    public String connectorType() {
                        return "SDI";
                    }
                },
                externalStatus -> com.acme.einvoice.domain.model.DocumentStatus.SUBMITTED
        );

        var connector = new SdiSubmissionConnector();

        var routingService = new DefaultRoutingService(
                new InMemoryCountryModuleRegistry(List.of(italyModule)),
                new InMemoryConnectorRegistry(List.of(connector)),
                new InMemoryTenantConfigurationService(Map.of(
                        tenantId,
                        new TenantFiscalConfiguration(
                                tenantId,
                                Set.of(italy),
                                Map.of(italy, new ConnectorBinding(italy, ConnectorId.of("SDI_DIRECT"))),
                                EnvironmentProfile.TEST,
                                new SignaturePolicy(false),
                                new ArchivePolicy(false)
                        )
                ))
        );

        FiscalDocumentRepository documentRepository = new InMemoryBootstrapDocumentRepository();
        SubmitDocumentService submitDocumentService = new SubmitDocumentService(
                documentRepository,
                routingService,
                new InMemoryArtifactStorage()
        );

        System.out.println("eInvoice platform bootstrap initialized: " + submitDocumentService.getClass().getSimpleName());
    }
}
