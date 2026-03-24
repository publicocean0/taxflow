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
        String status,
        Instant submittedAt,
        Instant updatedAt,
        Map<String, String> details
) {
}
