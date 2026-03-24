package com.acme.einvoice.domain.repository;

import com.acme.einvoice.domain.model.OutboxEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OutboxEventRepository {
    OutboxEvent save(OutboxEvent event);

    Optional<OutboxEvent> findById(String id);

    List<OutboxEvent> findProcessable(Instant now, int limit);

    boolean tryMarkProcessing(String eventId, Instant processingStartedAt);

    OutboxEvent markProcessed(String eventId, Instant processedAt);

    OutboxEvent markRetryableFailure(String eventId, String error, Instant nextAttemptAt);
}
