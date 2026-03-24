package com.acme.einvoice.application.service;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.NormalizedTransmissionStatusUpdate;
import com.acme.einvoice.domain.model.ReconciliationOutcome;
import com.acme.einvoice.domain.model.TransmissionExternalEvent;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;
import com.acme.einvoice.domain.model.TransmissionStatusUpdate;
import com.acme.einvoice.domain.repository.TransmissionExternalEventRepository;
import com.acme.einvoice.domain.repository.TransmissionRepository;
import com.acme.einvoice.domain.repository.TransmissionStatusUpdateRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ReconciliationProcessor {
    private final TransmissionExternalEventRepository externalEventRepository;
    private final TransmissionRepository transmissionRepository;
    private final TransmissionStatusUpdateRepository statusUpdateRepository;
    private final ExternalStatusTranslator translator;

    public ReconciliationProcessor(
            TransmissionExternalEventRepository externalEventRepository,
            TransmissionRepository transmissionRepository,
            TransmissionStatusUpdateRepository statusUpdateRepository,
            ExternalStatusTranslator translator
    ) {
        this.externalEventRepository = externalEventRepository;
        this.transmissionRepository = transmissionRepository;
        this.statusUpdateRepository = statusUpdateRepository;
        this.translator = translator;
    }

    public int reconcilePending(ServiceId serviceId, int maxBatchSize) {
        List<TransmissionExternalEvent> events = externalEventRepository.findUnprocessed(serviceId, maxBatchSize);
        int processed = 0;
        for (TransmissionExternalEvent event : events) {
            ReconciliationOutcome outcome = reconcileSingle(event);
            externalEventRepository.markProcessed(event.id(), outcome, outcome == ReconciliationOutcome.DUPLICATE, Instant.now());
            processed++;
        }
        return processed;
    }

    public ReconciliationOutcome reconcileSingle(TransmissionExternalEvent event) {
        Optional<TransmissionRecord> transmissionOpt = transmissionRepository.findById(event.transmissionId());
        if (transmissionOpt.isEmpty()) {
            return ReconciliationOutcome.TRANSMISSION_NOT_FOUND;
        }
        TransmissionRecord transmission = transmissionOpt.get();

        if (transmission.status().isTerminal()) {
            return ReconciliationOutcome.IGNORED_TERMINAL;
        }

        NormalizedTransmissionStatusUpdate normalized = translator.translate(transmission, event);

        if (isOutdated(transmission, normalized)) {
            return ReconciliationOutcome.IGNORED_OUTDATED;
        }

        TransmissionStatus targetStatus = normalized.targetStatus();
        if (transmission.status() == targetStatus) {
            writeAuditStatusUpdate(transmission, event, targetStatus);
            return ReconciliationOutcome.DUPLICATE;
        }

        TransmissionRecord updated = new TransmissionRecord(
                transmission.id(),
                transmission.serviceId(),
                transmission.documentId(),
                transmission.connectorId(),
                transmission.submissionIdempotencyKey(),
                event.externalReference().isPresent() ? event.externalReference() : transmission.externalReference(),
                targetStatus,
                transmission.submittedAt(),
                transmission.createdAt(),
                Instant.now(),
                transmission.processingAttempts(),
                Optional.empty(),
                transmission.lastErrorCode(),
                transmission.lastErrorMessage(),
                transmission.statusVersion() + 1,
                normalized.normalizedExternalStatusCode().isPresent() ? normalized.normalizedExternalStatusCode() : transmission.lastExternalStatusCode(),
                normalized.statusObservedAt().isPresent() ? normalized.statusObservedAt() : transmission.lastExternalStatusAt(),
                transmission.reconciliationVersion() + 1,
                Optional.of(Instant.now().plusSeconds(normalized.terminal() ? 0 : 300)),
                Optional.of(Instant.now()),
                transmission.details()
        );

        transmissionRepository.update(updated, transmission.statusVersion());
        writeAuditStatusUpdate(transmission, event, targetStatus);
        return ReconciliationOutcome.APPLIED;
    }

    private void writeAuditStatusUpdate(TransmissionRecord transmission, TransmissionExternalEvent event, TransmissionStatus targetStatus) {
        statusUpdateRepository.save(new TransmissionStatusUpdate(
                UUID.randomUUID().toString(),
                transmission.id(),
                event.sourceType(),
                event.externalStatusCode(),
                event.externalReference(),
                targetStatus,
                event.rawPayload(),
                event.receivedAt(),
                event.metadata()
        ));
    }

    private boolean isOutdated(TransmissionRecord transmission, NormalizedTransmissionStatusUpdate update) {
        if (update.statusObservedAt().isPresent() && transmission.lastExternalStatusAt().isPresent()) {
            return update.statusObservedAt().get().isBefore(transmission.lastExternalStatusAt().get());
        }
        return false;
    }
}
