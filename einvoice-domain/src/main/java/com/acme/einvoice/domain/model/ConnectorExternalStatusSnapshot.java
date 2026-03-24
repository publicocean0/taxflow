package com.acme.einvoice.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record ConnectorExternalStatusSnapshot(
        String sourceType,
        Optional<String> externalStatusCode,
        Optional<String> externalStatusLabel,
        Optional<String> externalReference,
        String deduplicationKey,
        Optional<String> rawPayload,
        Optional<Instant> occurredAtExternal,
        Instant receivedAt,
        Map<String, String> metadata
) {
}
