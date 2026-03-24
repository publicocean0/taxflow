package com.acme.einvoice.domain.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransmissionRepository {
    TransmissionRecord save(TransmissionRecord transmissionRecord);

    TransmissionRecord update(TransmissionRecord transmissionRecord, long expectedStatusVersion);

    Optional<TransmissionRecord> findById(String transmissionId);

    Optional<TransmissionRecord> findByIdempotencyKey(ServiceId serviceId, String documentId, String idempotencyKey);

    Optional<TransmissionRecord> findNextReadyForSubmission(String transmissionId);

    boolean tryMarkSubmitting(String transmissionId, long expectedStatusVersion);

    boolean isInStatus(String transmissionId, TransmissionStatus status);

    List<TransmissionRecord> findDueForStatusCheck(ServiceId serviceId, Instant now, int maxBatchSize);
}
