package com.acme.einvoice.connectors.spi;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record ExternalStatusResult(
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
