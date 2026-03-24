package com.acme.einvoice.domain.model;

import com.acme.einvoice.common.model.ServiceId;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record TransmissionExternalEvent(
        String id,
        ServiceId serviceId,
        Optional<String> tenantId,
        String transmissionId,
        Optional<String> documentId,
        String sourceType,
        Optional<String> externalStatusCode,
        Optional<String> externalStatusLabel,
        Optional<String> externalReference,
        String deduplicationKey,
        Optional<String> rawPayload,
        Optional<Instant> occurredAtExternal,
        Instant receivedAt,
        Optional<Instant> processedAt,
        Optional<ReconciliationOutcome> reconciliationOutcome,
        boolean duplicate,
        Instant createdAt,
        Map<String, String> metadata
) {
}
