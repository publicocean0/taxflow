package com.acme.einvoice.domain.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.TransmissionRecord;

import java.util.Optional;

public interface TransmissionRepository {
    TransmissionRecord save(TransmissionRecord transmissionRecord);

    Optional<TransmissionRecord> findByIdempotencyKey(ServiceId serviceId, String documentId, String idempotencyKey);
}
