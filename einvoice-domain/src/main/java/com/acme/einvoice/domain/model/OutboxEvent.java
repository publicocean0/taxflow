package com.acme.einvoice.domain.model;

import com.acme.einvoice.common.model.ServiceId;

import java.time.Instant;
import java.util.Optional;

public record OutboxEvent(
        String id,
        String aggregateType,
        String aggregateId,
        ServiceId serviceId,
        String eventType,
        String payload,
        OutboxEventStatus status,
        Instant createdAt,
        Optional<Instant> processingStartedAt,
        Optional<Instant> processedAt,
        int processingAttempts,
        Optional<Instant> nextAttemptAt,
        Optional<String> lastError
) {
}
