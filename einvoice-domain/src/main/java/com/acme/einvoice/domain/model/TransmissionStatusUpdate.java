package com.acme.einvoice.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record TransmissionStatusUpdate(
        String id,
        String transmissionId,
        String source,
        Optional<String> externalStatusCode,
        Optional<String> externalReference,
        TransmissionStatus mappedStatus,
        Optional<String> rawPayload,
        Instant receivedAt,
        Map<String, String> metadata
) {
}
