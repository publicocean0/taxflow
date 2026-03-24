package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.domain.model.OutboxEvent;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.repository.SubmissionIntentRepository;

public final class InMemorySubmissionIntentRepository implements SubmissionIntentRepository {
    private final InMemoryTransmissionRepository transmissionRepository;
    private final InMemoryOutboxEventRepository outboxEventRepository;

    public InMemorySubmissionIntentRepository(
            InMemoryTransmissionRepository transmissionRepository,
            InMemoryOutboxEventRepository outboxEventRepository
    ) {
        this.transmissionRepository = transmissionRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Override
    public synchronized void saveIntent(TransmissionRecord transmissionRecord, OutboxEvent outboxEvent) {
        transmissionRepository.save(transmissionRecord);
        try {
            outboxEventRepository.save(outboxEvent);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to persist submit intent transactionally", exception);
        }
    }
}
