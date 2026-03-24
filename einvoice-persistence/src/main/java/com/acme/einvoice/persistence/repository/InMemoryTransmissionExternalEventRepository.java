package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.ReconciliationOutcome;
import com.acme.einvoice.domain.model.TransmissionExternalEvent;
import com.acme.einvoice.domain.repository.TransmissionExternalEventRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTransmissionExternalEventRepository implements TransmissionExternalEventRepository {
    private final Map<String, TransmissionExternalEvent> events = new ConcurrentHashMap<>();
    private final Map<EventDeduplicationKey, String> eventByDedup = new ConcurrentHashMap<>();

    @Override
    public TransmissionExternalEvent save(TransmissionExternalEvent event) {
        EventDeduplicationKey dedupKey = new EventDeduplicationKey(event.serviceId(), event.deduplicationKey());
        String existingId = eventByDedup.putIfAbsent(dedupKey, event.id());
        if (existingId != null) {
            return events.get(existingId);
        }
        events.put(event.id(), event);
        return event;
    }

    @Override
    public Optional<TransmissionExternalEvent> findByDeduplicationKey(ServiceId serviceId, String deduplicationKey) {
        String eventId = eventByDedup.get(new EventDeduplicationKey(serviceId, deduplicationKey));
        return Optional.ofNullable(eventId).map(events::get);
    }

    @Override
    public List<TransmissionExternalEvent> findUnprocessed(ServiceId serviceId, int maxBatchSize) {
        return events.values().stream()
                .filter(event -> event.serviceId().equals(serviceId))
                .filter(event -> event.processedAt().isEmpty())
                .sorted(Comparator.comparing(TransmissionExternalEvent::receivedAt))
                .limit(maxBatchSize)
                .toList();
    }

    @Override
    public List<TransmissionExternalEvent> findByTransmissionId(String transmissionId) {
        return events.values().stream()
                .filter(event -> event.transmissionId().equals(transmissionId))
                .sorted(Comparator.comparing(TransmissionExternalEvent::receivedAt))
                .toList();
    }

    @Override
    public void markProcessed(String eventId, ReconciliationOutcome outcome, boolean duplicate, Instant processedAt) {
        events.computeIfPresent(eventId, (id, existing) -> new TransmissionExternalEvent(
                existing.id(),
                existing.serviceId(),
                existing.tenantId(),
                existing.transmissionId(),
                existing.documentId(),
                existing.sourceType(),
                existing.externalStatusCode(),
                existing.externalStatusLabel(),
                existing.externalReference(),
                existing.deduplicationKey(),
                existing.rawPayload(),
                existing.occurredAtExternal(),
                existing.receivedAt(),
                Optional.of(processedAt),
                Optional.of(outcome),
                duplicate,
                existing.createdAt(),
                existing.metadata()
        ));
    }

    private record EventDeduplicationKey(ServiceId serviceId, String deduplicationKey) {
    }
}
