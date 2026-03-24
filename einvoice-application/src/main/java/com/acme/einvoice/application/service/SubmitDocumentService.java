package com.acme.einvoice.application.service;

import com.acme.einvoice.application.artifact.Artifact;
import com.acme.einvoice.application.artifact.ArtifactReference;
import com.acme.einvoice.application.artifact.ArtifactStorage;
import com.acme.einvoice.application.ecosystem.EcosystemDirectoryService;
import com.acme.einvoice.application.exception.DocumentNotFoundException;
import com.acme.einvoice.application.exception.ServiceNotFoundException;
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
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;
import com.acme.einvoice.domain.repository.TransmissionRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SubmitDocumentService implements SubmitDocumentUseCase {
    private final FiscalDocumentRepository documentRepository;
    private final TransmissionRepository transmissionRepository;
    private final RoutingService routingService;
    private final ArtifactStorage artifactStorage;
    private final EcosystemDirectoryService ecosystemDirectoryService;

    public SubmitDocumentService(
            FiscalDocumentRepository documentRepository,
            TransmissionRepository transmissionRepository,
            RoutingService routingService,
            ArtifactStorage artifactStorage,
            EcosystemDirectoryService ecosystemDirectoryService
    ) {
        this.documentRepository = documentRepository;
        this.transmissionRepository = transmissionRepository;
        this.routingService = routingService;
        this.artifactStorage = artifactStorage;
        this.ecosystemDirectoryService = ecosystemDirectoryService;
    }

    @Override
    public SubmitDocumentResult execute(SubmitDocumentCommand command) {
        var serviceRef = ecosystemDirectoryService.getService(command.serviceId())
                .orElseThrow(() -> new ServiceNotFoundException(null, command.serviceId()));

        FiscalDocument document = documentRepository
                .findById(command.serviceId(), command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));

        String idempotencyKey = command.idempotencyKey().orElseGet(() -> deriveIdempotencyKey(command, document));
        Optional<TransmissionRecord> existing = transmissionRepository
                .findByIdempotencyKey(command.serviceId(), document.id(), idempotencyKey);

        if (existing.isPresent()) {
            TransmissionRecord transmission = existing.get();
            return new SubmitDocumentResult(document.id(), transmission.id(), transmission.status(), transmission.submittedAt(),
                    transmission.externalReference(), Optional.empty(), java.util.List.of(), true);
        }

        CountryModule countryModule = routingService.resolveCountryModule(document.countryCode());
        ValidationReport report = countryModule.validator().validate(document, CountryContext.of(command.serviceId()));
        if (!report.isValid()) {
            throw new ValidationFailedException(document.id(), report);
        }

        RenderedDocument rendered = countryModule.renderer().render(document, CountryContext.of(command.serviceId()));
        ArtifactReference artifactReference = storeRenderedArtifact(rendered);

        SubmissionConnector connector = routingService.resolveConnector(command.serviceId(), document.countryCode());
        SubmissionResult submissionResult = submit(connector, command, document, rendered);

        TransmissionRecord persisted = transmissionRepository.save(new TransmissionRecord(
                submissionResult.transmissionId(),
                command.serviceId(),
                document.id(),
                connector.id().value(),
                idempotencyKey,
                submissionResult.externalReference(),
                submissionResult.outcome(),
                submissionResult.submittedAt(),
                Instant.now(),
                Map.of("connectorType", connector.type(), "tenantId", serviceRef.tenantId().value().toString())
        ));

        return new SubmitDocumentResult(
                document.id(),
                persisted.id(),
                persisted.status(),
                persisted.submittedAt(),
                persisted.externalReference(),
                Optional.ofNullable(artifactReference),
                report.warnings(),
                false
        );
    }

    private String deriveIdempotencyKey(SubmitDocumentCommand command, FiscalDocument document) {
        return command.serviceId().value() + ":" + document.id();
    }

    private SubmissionResult submit(
            SubmissionConnector connector,
            SubmitDocumentCommand command,
            FiscalDocument document,
            RenderedDocument rendered
    ) {
        try {
            SubmissionResult result = connector.submit(new SubmissionCommand(
                    command.serviceId(),
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
                new ArtifactReference(UUID.randomUUID().toString(), rendered.format()),
                rendered.payload(),
                rendered.metadata(),
                Instant.now()
        );
        return artifactStorage.store(artifact);
    }
}
