package com.acme.einvoice.domain.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.ReconciliationOutcome;
import com.acme.einvoice.domain.model.TransmissionExternalEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransmissionExternalEventRepository {
    TransmissionExternalEvent save(TransmissionExternalEvent event);

    Optional<TransmissionExternalEvent> findByDeduplicationKey(ServiceId serviceId, String deduplicationKey);

    List<TransmissionExternalEvent> findUnprocessed(ServiceId serviceId, int maxBatchSize);

    List<TransmissionExternalEvent> findByTransmissionId(String transmissionId);

    void markProcessed(String eventId, ReconciliationOutcome outcome, boolean duplicate, Instant processedAt);
}
