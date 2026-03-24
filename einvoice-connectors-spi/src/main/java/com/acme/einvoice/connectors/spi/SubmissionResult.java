package com.acme.einvoice.connectors.spi;

import java.time.Instant;
import java.util.Optional;

public record SubmissionResult(
        String transmissionId,
        String outcome,
        Optional<String> externalReference,
        Instant submittedAt
) {
}
