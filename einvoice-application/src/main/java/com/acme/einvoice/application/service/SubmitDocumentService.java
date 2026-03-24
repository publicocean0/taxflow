package com.acme.einvoice.application.service;

import com.acme.einvoice.application.artifact.Artifact;
import com.acme.einvoice.application.artifact.ArtifactReference;
import com.acme.einvoice.application.artifact.ArtifactStorage;
import com.acme.einvoice.application.ecosystem.EcosystemDirectoryService;
import com.acme.einvoice.application.exception.DocumentNotFoundException;
import com.acme.einvoice.application.exception.ServiceNotFoundException;
import com.acme.einvoice.application.exception.ValidationFailedException;
import com.acme.einvoice.application.routing.RoutingService;
import com.acme.einvoice.application.usecase.SubmitDocumentCommand;
import com.acme.einvoice.application.usecase.SubmitDocumentResult;
import com.acme.einvoice.application.usecase.SubmitDocumentUseCase;
import com.acme.einvoice.country.spi.CountryContext;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.RenderedDocument;
import com.acme.einvoice.country.spi.ValidationReport;
import com.acme.einvoice.domain.model.OutboxEvent;
import com.acme.einvoice.domain.model.OutboxEventStatus;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;
import com.acme.einvoice.domain.repository.SubmissionIntentRepository;
import com.acme.einvoice.domain.repository.TransmissionRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SubmitDocumentService implements SubmitDocumentUseCase {
    public static final String SUBMISSION_REQUESTED = "SubmissionRequested";
    private final FiscalDocumentRepository documentRepository;
    private final TransmissionRepository transmissionRepository;
    private final SubmissionIntentRepository submissionIntentRepository;
    private final RoutingService routingService;
    private final ArtifactStorage artifactStorage;
    private final EcosystemDirectoryService ecosystemDirectoryService;

    public SubmitDocumentService(
            FiscalDocumentRepository documentRepository,
            TransmissionRepository transmissionRepository,
            SubmissionIntentRepository submissionIntentRepository,
            RoutingService routingService,
            ArtifactStorage artifactStorage,
            EcosystemDirectoryService ecosystemDirectoryService
    ) {
        this.documentRepository = documentRepository;
        this.transmissionRepository = transmissionRepository;
        this.submissionIntentRepository = submissionIntentRepository;
        this.routingService = routingService;
        this.artifactStorage = artifactStorage;
        this.ecosystemDirectoryService = ecosystemDirectoryService;
    }

    @Override
    public SubmitDocumentResult execute(SubmitDocumentCommand command) {
        var serviceRef = ecosystemDirectoryService.getService(command.serviceId())
                .orElseThrow(() -> new ServiceNotFoundException(null, command.serviceId()));

        var document = documentRepository
                .findById(command.serviceId(), command.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(command.documentId()));

        String idempotencyKey = command.idempotencyKey().orElseGet(() -> deriveIdempotencyKey(command, document.id()));
        Optional<TransmissionRecord> existing = transmissionRepository
                .findByIdempotencyKey(command.serviceId(), document.id(), idempotencyKey);

        if (existing.isPresent()) {
            TransmissionRecord transmission = existing.get();
            return new SubmitDocumentResult(document.id(), transmission.id(), transmission.status().name(), transmission.submittedAt(),
                    transmission.externalReference(), Optional.empty(), java.util.List.of(), true);
        }

        CountryModule countryModule = routingService.resolveCountryModule(document.countryCode());
        ValidationReport report = countryModule.validator().validate(document, CountryContext.of(command.serviceId()));
        if (!report.isValid()) {
            throw new ValidationFailedException(document.id(), report);
        }

        RenderedDocument rendered = countryModule.renderer().render(document, CountryContext.of(command.serviceId()));
        ArtifactReference artifactReference = storeRenderedArtifact(rendered);

        Instant now = Instant.now();
        String transmissionId = UUID.randomUUID().toString();
        TransmissionRecord transmission = new TransmissionRecord(
                transmissionId,
                command.serviceId(),
                document.id(),
                routingService.resolveConnector(command.serviceId(), document.countryCode()).id().value(),
                idempotencyKey,
                Optional.empty(),
                TransmissionStatus.PENDING_SUBMISSION,
                Optional.empty(),
                now,
                now,
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                Map.of("tenantId", serviceRef.tenantId().value().toString())
        );

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID().toString(),
                "TransmissionRecord",
                transmissionId,
                command.serviceId(),
                SUBMISSION_REQUESTED,
                new SubmissionRequestedEventPayload(transmissionId, command.serviceId(), document.id()).serialize(),
                OutboxEventStatus.PENDING,
                now,
                Optional.empty(),
                Optional.empty(),
                0,
                Optional.empty(),
                Optional.empty()
        );

        submissionIntentRepository.saveIntent(transmission, event);

        return new SubmitDocumentResult(
                document.id(),
                transmission.id(),
                transmission.status().name(),
                transmission.submittedAt(),
                transmission.externalReference(),
                Optional.ofNullable(artifactReference),
                report.warnings(),
                false
        );
    }

    private String deriveIdempotencyKey(SubmitDocumentCommand command, String documentId) {
        return command.serviceId().value() + ":" + documentId;
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
