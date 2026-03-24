package com.acme.einvoice.application.service;

import com.acme.einvoice.domain.model.ReconciliationOutcome;
import com.acme.einvoice.domain.model.TransmissionExternalEvent;
import com.acme.einvoice.domain.repository.TransmissionExternalEventRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ConnectorWebhookIngestionService implements IngestExternalStatusUpdateUseCase {
    private final TransmissionExternalEventRepository externalEventRepository;
    private final ReconciliationProcessor reconciliationProcessor;

    public ConnectorWebhookIngestionService(
            TransmissionExternalEventRepository externalEventRepository,
            ReconciliationProcessor reconciliationProcessor
    ) {
        this.externalEventRepository = externalEventRepository;
        this.reconciliationProcessor = reconciliationProcessor;
    }

    @Override
    public ReconciliationOutcome ingest(IngestExternalStatusUpdateCommand command) {
        Optional<TransmissionExternalEvent> existing = externalEventRepository.findByDeduplicationKey(command.serviceId(), command.deduplicationKey());
        if (existing.isPresent()) {
            return ReconciliationOutcome.DUPLICATE;
        }

        Instant now = Instant.now();
        TransmissionExternalEvent event = new TransmissionExternalEvent(
                UUID.randomUUID().toString(),
                command.serviceId(),
                Optional.empty(),
                command.transmissionId(),
                Optional.empty(),
                command.sourceType(),
                Optional.ofNullable(command.externalStatusCode()),
                Optional.ofNullable(command.externalStatusLabel()),
                Optional.ofNullable(command.externalReference()),
                command.deduplicationKey(),
                Optional.ofNullable(command.rawPayload()),
                Optional.empty(),
                now,
                Optional.empty(),
                Optional.empty(),
                false,
                now,
                Map.of()
        );

        externalEventRepository.save(event);
        ReconciliationOutcome outcome = reconciliationProcessor.reconcileSingle(event);
        externalEventRepository.markProcessed(event.id(), outcome, false, Instant.now());
        return outcome;
    }
}
