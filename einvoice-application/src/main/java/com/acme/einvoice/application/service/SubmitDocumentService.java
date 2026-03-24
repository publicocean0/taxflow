package com.acme.einvoice.application.service;

import com.acme.einvoice.application.artifact.Artifact;
import com.acme.einvoice.application.artifact.ArtifactReference;
import com.acme.einvoice.application.artifact.ArtifactStorage;
import com.acme.einvoice.application.exception.DocumentNotFoundException;
import com.acme.einvoice.application.exception.SubmissionFailedException;
import com.acme.einvoice.application.exception.ValidationFailedException;
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
import java.util.Optional;

public final class SubmitDocumentService implements SubmitDocumentUseCase {
    private final FiscalDocumentRepository documentRepository;
    private final RoutingService routingService;
    private final ArtifactStorage artifactStorage;

    public SubmitDocumentService(
            FiscalDocumentRepository documentRepository,
            RoutingService routingService,
            ArtifactStorage artifactStorage
    ) {
        this.documentRepository = documentRepository;
        this.routingService = routingService;
        this.artifactStorage = artifactStorage;
    }

    @Override
    public SubmitDocumentResult execute(SubmitDocumentCommand command) {
        FiscalDocument document = documentRepository.findById(command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));

        CountryModule countryModule = routingService.resolveCountryModule(document.countryCode());
        ValidationReport report = countryModule.validator().validate(document, CountryContext.of(command.tenantId()));

        if (!report.isValid()) {
            throw new ValidationFailedException(document.id(), report);
        }

        RenderedDocument rendered = countryModule.renderer().render(document, CountryContext.of(command.tenantId()));
        ArtifactReference artifactReference = storeRenderedArtifact(rendered);

        SubmissionConnector connector = routingService.resolveConnector(command.tenantId(), document.countryCode());
        SubmissionResult submissionResult = submit(connector, command, document, rendered);

        return new SubmitDocumentResult(
                document.id(),
                submissionResult.transmissionId(),
                submissionResult.outcome(),
                submissionResult.submittedAt(),
                submissionResult.externalReference(),
                Optional.ofNullable(artifactReference),
                report.warnings()
        );
    }

    private SubmissionResult submit(
            SubmissionConnector connector,
            SubmitDocumentCommand command,
            FiscalDocument document,
            RenderedDocument rendered
    ) {
        try {
            SubmissionResult result = connector.submit(new SubmissionCommand(
                    command.tenantId(),
                    document.countryCode(),
                    document.id(),
                    rendered == null ? new RenderedDocument("UNKNOWN", new byte[0], Map.of()) : rendered
            ));

            if (result == null) {
                throw new SubmissionFailedException("Submission connector returned null result");
            }

            return result;
        } catch (RuntimeException exception) {
            if (exception instanceof SubmissionFailedException) {
                throw exception;
            }
            throw new SubmissionFailedException("Submission failed for document " + document.id(), exception);
        }
    }

    private ArtifactReference storeRenderedArtifact(RenderedDocument rendered) {
        if (rendered == null || rendered.payload() == null) {
            return null;
        }
        Artifact artifact = new Artifact(
                new ArtifactReference(null, rendered.format()),
                rendered.payload(),
                rendered.metadata(),
                Instant.now()
        );
        return artifactStorage.store(artifact);
    }
}
