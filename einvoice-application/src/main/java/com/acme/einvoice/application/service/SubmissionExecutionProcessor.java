package com.acme.einvoice.application.service;

import com.acme.einvoice.application.exception.DocumentNotFoundException;
import com.acme.einvoice.application.routing.RoutingService;
import com.acme.einvoice.connectors.spi.SubmissionCommand;
import com.acme.einvoice.connectors.spi.SubmissionResult;
import com.acme.einvoice.country.spi.CountryContext;
import com.acme.einvoice.domain.model.OutboxEvent;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;
import com.acme.einvoice.domain.model.TransmissionStatusUpdate;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;
import com.acme.einvoice.domain.repository.OutboxEventRepository;
import com.acme.einvoice.domain.repository.TransmissionRepository;
import com.acme.einvoice.domain.repository.TransmissionStatusUpdateRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SubmissionExecutionProcessor {
    private final OutboxEventRepository outboxEventRepository;
    private final TransmissionRepository transmissionRepository;
    private final TransmissionStatusUpdateRepository statusUpdateRepository;
    private final FiscalDocumentRepository documentRepository;
    private final RoutingService routingService;

    public SubmissionExecutionProcessor(
            OutboxEventRepository outboxEventRepository,
            TransmissionRepository transmissionRepository,
            TransmissionStatusUpdateRepository statusUpdateRepository,
            FiscalDocumentRepository documentRepository,
            RoutingService routingService
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.transmissionRepository = transmissionRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.documentRepository = documentRepository;
        this.routingService = routingService;
    }

    public int processPendingSubmissions(int maxBatchSize) {
        int processed = 0;
        for (OutboxEvent event : outboxEventRepository.findProcessable(Instant.now(), maxBatchSize)) {
            if (!SubmitDocumentService.SUBMISSION_REQUESTED.equals(event.eventType())) {
                continue;
            }
            if (!outboxEventRepository.tryMarkProcessing(event.id(), Instant.now())) {
                continue;
            }
            processSingle(event);
            processed++;
        }
        return processed;
    }

    private void processSingle(OutboxEvent event) {
        SubmissionRequestedEventPayload payload = SubmissionRequestedEventPayload.deserialize(event.payload());
        Optional<TransmissionRecord> transmissionOpt = transmissionRepository.findById(payload.transmissionId());
        if (transmissionOpt.isEmpty()) {
            outboxEventRepository.markProcessed(event.id(), Instant.now());
            return;
        }

        TransmissionRecord transmission = transmissionOpt.get();
        if (transmission.status().isTerminal() || transmission.status() == TransmissionStatus.SUBMITTED || transmission.status() == TransmissionStatus.STATUS_PENDING) {
            outboxEventRepository.markProcessed(event.id(), Instant.now());
            return;
        }

        if (!transmissionRepository.tryMarkSubmitting(transmission.id(), transmission.statusVersion())) {
            return;
        }

        TransmissionRecord submitting = transmissionRepository.findById(transmission.id()).orElseThrow();

        try {
            var document = documentRepository.findById(payload.serviceId(), payload.documentId())
                    .orElseThrow(() -> new DocumentNotFoundException(payload.documentId()));
            var connector = routingService.resolveConnector(payload.serviceId(), document.countryCode());
            var country = routingService.resolveCountryModule(document.countryCode());
            var rendered = country.renderer().render(document, CountryContext.of(payload.serviceId()));

            SubmissionResult result = connector.submit(new SubmissionCommand(
                    payload.serviceId(),
                    document.countryCode(),
                    document.id(),
                    rendered
            ));

            TransmissionRecord submitted = new TransmissionRecord(
                    submitting.id(),
                    submitting.serviceId(),
                    submitting.documentId(),
                    submitting.connectorId(),
                    submitting.submissionIdempotencyKey(),
                    result.externalReference(),
                    TransmissionStatus.SUBMITTED,
                    Optional.ofNullable(result.submittedAt()),
                    submitting.createdAt(),
                    Instant.now(),
                    submitting.processingAttempts(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    submitting.statusVersion() + 1,
                    submitting.details()
            );
            transmissionRepository.update(submitted, submitting.statusVersion());
            statusUpdateRepository.save(new TransmissionStatusUpdate(
                    UUID.randomUUID().toString(),
                    submitting.id(),
                    "CONNECTOR_SUBMIT",
                    Optional.ofNullable(result.outcome()),
                    result.externalReference(),
                    TransmissionStatus.SUBMITTED,
                    Optional.empty(),
                    Instant.now(),
                    Map.of()
            ));
            outboxEventRepository.markProcessed(event.id(), Instant.now());
        } catch (RuntimeException exception) {
            TransmissionStatus failureStatus = isRetryable(exception) ? TransmissionStatus.FAILED_RETRYABLE : TransmissionStatus.FAILED_FINAL;
            TransmissionRecord failed = new TransmissionRecord(
                    submitting.id(),
                    submitting.serviceId(),
                    submitting.documentId(),
                    submitting.connectorId(),
                    submitting.submissionIdempotencyKey(),
                    submitting.externalReference(),
                    failureStatus,
                    submitting.submittedAt(),
                    submitting.createdAt(),
                    Instant.now(),
                    submitting.processingAttempts(),
                    failureStatus == TransmissionStatus.FAILED_RETRYABLE ? Optional.of(Instant.now().plusSeconds(60)) : Optional.empty(),
                    Optional.of("SUBMISSION_ERROR"),
                    Optional.ofNullable(exception.getMessage()),
                    submitting.statusVersion() + 1,
                    submitting.details()
            );
            transmissionRepository.update(failed, submitting.statusVersion());
            if (failureStatus == TransmissionStatus.FAILED_RETRYABLE) {
                outboxEventRepository.markRetryableFailure(event.id(), exception.getMessage(), Instant.now().plusSeconds(60));
            } else {
                outboxEventRepository.markProcessed(event.id(), Instant.now());
            }
        }
    }

    private boolean isRetryable(RuntimeException exception) {
        return !(exception instanceof IllegalArgumentException);
    }
}
