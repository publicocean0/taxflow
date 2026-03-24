package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;
import com.acme.einvoice.domain.repository.TransmissionRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTransmissionRepository implements TransmissionRepository {
    private final Map<TransmissionUniqKey, String> transmissionIdByIdempotency = new ConcurrentHashMap<>();
    private final Map<String, TransmissionRecord> transmissionsById = new ConcurrentHashMap<>();

    @Override
    public TransmissionRecord save(TransmissionRecord transmissionRecord) {
        TransmissionUniqKey key = new TransmissionUniqKey(
                transmissionRecord.serviceId(),
                transmissionRecord.documentId(),
                transmissionRecord.submissionIdempotencyKey()
        );

        String existingTransmissionId = transmissionIdByIdempotency.putIfAbsent(key, transmissionRecord.id());
        if (existingTransmissionId != null) {
            throw new IllegalStateException("Duplicate submission for idempotency key " + transmissionRecord.submissionIdempotencyKey());
        }

        transmissionsById.put(transmissionRecord.id(), transmissionRecord);
        return transmissionRecord;
    }

    @Override
    public TransmissionRecord update(TransmissionRecord transmissionRecord, long expectedStatusVersion) {
        return transmissionsById.compute(transmissionRecord.id(), (id, existing) -> {
            if (existing == null) {
                throw new IllegalStateException("Transmission not found: " + transmissionRecord.id());
            }
            if (existing.statusVersion() != expectedStatusVersion) {
                throw new IllegalStateException("Optimistic conflict on transmission " + transmissionRecord.id());
            }
            return transmissionRecord;
        });
    }

    @Override
    public Optional<TransmissionRecord> findById(String transmissionId) {
        return Optional.ofNullable(transmissionsById.get(transmissionId));
    }

    @Override
    public Optional<TransmissionRecord> findByIdempotencyKey(ServiceId serviceId, String documentId, String idempotencyKey) {
        String transmissionId = transmissionIdByIdempotency.get(new TransmissionUniqKey(serviceId, documentId, idempotencyKey));
        return Optional.ofNullable(transmissionId).flatMap(this::findById);
    }

    @Override
    public Optional<TransmissionRecord> findNextReadyForSubmission(String transmissionId) {
        return findById(transmissionId)
                .filter(record -> record.status() == TransmissionStatus.PENDING_SUBMISSION || record.status() == TransmissionStatus.FAILED_RETRYABLE);
    }

    @Override
    public boolean tryMarkSubmitting(String transmissionId, long expectedStatusVersion) {
        return transmissionsById.computeIfPresent(transmissionId, (id, existing) -> {
            if (existing.statusVersion() != expectedStatusVersion) {
                return existing;
            }
            if (existing.status() != TransmissionStatus.PENDING_SUBMISSION && existing.status() != TransmissionStatus.FAILED_RETRYABLE) {
                return existing;
            }
            return new TransmissionRecord(
                    existing.id(),
                    existing.serviceId(),
                    existing.documentId(),
                    existing.connectorId(),
                    existing.submissionIdempotencyKey(),
                    existing.externalReference(),
                    TransmissionStatus.SUBMITTING,
                    existing.submittedAt(),
                    existing.createdAt(),
                    existing.updatedAt(),
                    existing.processingAttempts() + 1,
                    Optional.empty(),
                    existing.lastErrorCode(),
                    existing.lastErrorMessage(),
                    existing.statusVersion() + 1,
                    existing.lastExternalStatusCode(),
                    existing.lastExternalStatusAt(),
                    existing.reconciliationVersion(),
                    existing.nextStatusCheckAt(),
                    existing.statusLastCheckedAt(),
                    existing.details()
            );
        }).status() == TransmissionStatus.SUBMITTING;
    }

    @Override
    public boolean isInStatus(String transmissionId, TransmissionStatus status) {
        return findById(transmissionId).map(TransmissionRecord::status).filter(s -> s == status).isPresent();
    }

    @Override
    public List<TransmissionRecord> findDueForStatusCheck(ServiceId serviceId, Instant now, int maxBatchSize) {
        return transmissionsById.values().stream()
                .filter(record -> record.serviceId().equals(serviceId))
                .filter(record -> !record.status().isTerminal())
                .filter(record -> record.status() == TransmissionStatus.SUBMITTED || record.status() == TransmissionStatus.STATUS_PENDING)
                .filter(record -> record.nextStatusCheckAt().isEmpty() || !record.nextStatusCheckAt().get().isAfter(now))
                .sorted(Comparator.comparing(TransmissionRecord::updatedAt))
                .limit(maxBatchSize)
                .toList();
    }

    private record TransmissionUniqKey(ServiceId serviceId, String documentId, String idempotencyKey) {
    }
}
