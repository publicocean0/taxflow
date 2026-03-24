package com.acme.einvoice.domain.model;

import com.acme.einvoice.common.model.ServiceId;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record TransmissionRecord(
        String id,
        ServiceId serviceId,
        String documentId,
        String connectorId,
        String submissionIdempotencyKey,
        Optional<String> externalReference,
        TransmissionStatus status,
        Optional<Instant> submittedAt,
        Instant createdAt,
        Instant updatedAt,
        int processingAttempts,
        Optional<Instant> nextAttemptAt,
        Optional<String> lastErrorCode,
        Optional<String> lastErrorMessage,
        long statusVersion,
        Map<String, String> details
) {
}
