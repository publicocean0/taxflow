package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.repository.TransmissionRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTransmissionRepository implements TransmissionRepository {
    private final Map<TransmissionUniqKey, TransmissionRecord> transmissionsByIdempotency = new ConcurrentHashMap<>();

    @Override
    public TransmissionRecord save(TransmissionRecord transmissionRecord) {
        TransmissionUniqKey key = new TransmissionUniqKey(
                transmissionRecord.serviceId(),
                transmissionRecord.documentId(),
                transmissionRecord.submissionIdempotencyKey()
        );
        TransmissionRecord existing = transmissionsByIdempotency.putIfAbsent(key, transmissionRecord);
        if (existing != null) {
            throw new IllegalStateException("Duplicate submission for idempotency key " + transmissionRecord.submissionIdempotencyKey());
        }
        return transmissionRecord;
    }

    @Override
    public Optional<TransmissionRecord> findByIdempotencyKey(ServiceId serviceId, String documentId, String idempotencyKey) {
        return Optional.ofNullable(transmissionsByIdempotency.get(new TransmissionUniqKey(serviceId, documentId, idempotencyKey)));
    }

    private record TransmissionUniqKey(ServiceId serviceId, String documentId, String idempotencyKey) {
    }
}
