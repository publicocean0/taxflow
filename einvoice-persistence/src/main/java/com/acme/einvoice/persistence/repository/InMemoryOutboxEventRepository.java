package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.domain.model.OutboxEvent;
import com.acme.einvoice.domain.model.OutboxEventStatus;
import com.acme.einvoice.domain.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryOutboxEventRepository implements OutboxEventRepository {
    private final ConcurrentMap<String, OutboxEvent> events = new ConcurrentHashMap<>();

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEvent existing = events.putIfAbsent(event.id(), event);
        if (existing != null) {
            throw new IllegalStateException("Outbox event already exists: " + event.id());
        }
        return event;
    }

    @Override
    public Optional<OutboxEvent> findById(String id) {
        return Optional.ofNullable(events.get(id));
    }

    @Override
    public List<OutboxEvent> findProcessable(Instant now, int limit) {
        return events.values().stream()
                .filter(event -> event.status() == OutboxEventStatus.PENDING ||
                        (event.status() == OutboxEventStatus.PROCESSING && event.nextAttemptAt().map(t -> !t.isAfter(now)).orElse(false)))
                .sorted(Comparator.comparing(OutboxEvent::createdAt))
                .limit(limit)
                .toList();
    }

    @Override
    public boolean tryMarkProcessing(String eventId, Instant processingStartedAt) {
        return events.computeIfPresent(eventId, (id, event) -> {
            if (event.status() == OutboxEventStatus.PROCESSED) {
                return event;
            }
            return new OutboxEvent(
                    event.id(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.serviceId(),
                    event.eventType(),
                    event.payload(),
                    OutboxEventStatus.PROCESSING,
                    event.createdAt(),
                    Optional.of(processingStartedAt),
                    Optional.empty(),
                    event.processingAttempts() + 1,
                    event.nextAttemptAt(),
                    event.lastError()
            );
        }).status() == OutboxEventStatus.PROCESSING;
    }

    @Override
    public OutboxEvent markProcessed(String eventId, Instant processedAt) {
        return events.compute(eventId, (id, event) -> {
            if (event == null) {
                throw new IllegalStateException("Outbox event not found: " + eventId);
            }
            return new OutboxEvent(
                    event.id(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.serviceId(),
                    event.eventType(),
                    event.payload(),
                    OutboxEventStatus.PROCESSED,
                    event.createdAt(),
                    event.processingStartedAt(),
                    Optional.of(processedAt),
                    event.processingAttempts(),
                    Optional.empty(),
                    event.lastError()
            );
        });
    }

    @Override
    public OutboxEvent markRetryableFailure(String eventId, String error, Instant nextAttemptAt) {
        return events.compute(eventId, (id, event) -> {
            if (event == null) {
                throw new IllegalStateException("Outbox event not found: " + eventId);
            }
            return new OutboxEvent(
                    event.id(),
                    event.aggregateType(),
                    event.aggregateId(),
                    event.serviceId(),
                    event.eventType(),
                    event.payload(),
                    OutboxEventStatus.PENDING,
                    event.createdAt(),
                    Optional.empty(),
                    Optional.empty(),
                    event.processingAttempts(),
                    Optional.of(nextAttemptAt),
                    Optional.ofNullable(error)
            );
        });
    }
}
