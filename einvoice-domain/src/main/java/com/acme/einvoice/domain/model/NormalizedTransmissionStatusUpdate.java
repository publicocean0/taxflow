package com.acme.einvoice.domain.model;

import java.time.Instant;
import java.util.Optional;

public record NormalizedTransmissionStatusUpdate(
        TransmissionStatus targetStatus,
        Optional<String> normalizedExternalStatusCode,
        Optional<Instant> statusObservedAt,
        boolean terminal,
        String reason
) {
}
