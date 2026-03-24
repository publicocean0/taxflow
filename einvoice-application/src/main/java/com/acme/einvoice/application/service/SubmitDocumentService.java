package com.acme.einvoice.application.service;

import com.acme.einvoice.application.routing.RoutingService;
import com.acme.einvoice.application.usecase.SubmitDocumentCommand;
import com.acme.einvoice.application.usecase.SubmitDocumentResult;
import com.acme.einvoice.application.usecase.SubmitDocumentUseCase;
import com.acme.einvoice.connectors.spi.SubmissionCommand;
import com.acme.einvoice.connectors.spi.SubmissionConnector;
import com.acme.einvoice.connectors.spi.SubmissionResult;
import com.acme.einvoice.country.spi.CountryContext;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.RenderedDocument;
import com.acme.einvoice.country.spi.ValidationReport;
import com.acme.einvoice.domain.model.FiscalDocument;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;

import java.time.Instant;
import java.util.Map;

public final class SubmitDocumentService implements SubmitDocumentUseCase {
    private final FiscalDocumentRepository documentRepository;
    private final RoutingService routingService;

    public SubmitDocumentService(FiscalDocumentRepository documentRepository, RoutingService routingService) {
        this.documentRepository = documentRepository;
        this.routingService = routingService;
    }

    @Override
    public SubmitDocumentResult execute(SubmitDocumentCommand command) {
        FiscalDocument document = documentRepository.findById(command.documentId())
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + command.documentId()));

        CountryModule countryModule = routingService.resolveCountryModule(document.countryCode());
        ValidationReport report = countryModule.validator().validate(document, CountryContext.of(command.tenantId()));

        if (!report.valid()) {
            throw new IllegalStateException("Validation failed: " + report.errors());
        }

        RenderedDocument rendered = countryModule.renderer().render(document, CountryContext.of(command.tenantId()));
        SubmissionConnector connector = routingService.resolveConnector(command.tenantId(), document.countryCode());

        SubmissionResult result = connector.submit(new SubmissionCommand(
                command.tenantId(),
                document.countryCode(),
                document.id(),
                rendered == null ? new RenderedDocument("UNKNOWN", new byte[0], Map.of()) : rendered
        ));

        SubmissionResult effectiveResult = result == null
                ? new SubmissionResult("N/A", "UNKNOWN", java.util.Optional.empty(), Instant.now())
                : result;

        return new SubmitDocumentResult(effectiveResult.transmissionId(), effectiveResult.outcome());
    }
}
