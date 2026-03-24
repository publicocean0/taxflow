package com.acme.einvoice.application.usecase;

import com.acme.einvoice.application.artifact.ArtifactReference;
import com.acme.einvoice.country.spi.ValidationMessage;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record SubmitDocumentResult(
        String documentId,
        String transmissionId,
        String outcome,
        Instant submittedAt,
        Optional<String> externalReference,
        Optional<ArtifactReference> renderedArtifact,
        List<ValidationMessage> warnings,
        boolean idempotentReplay
) {
}
