package com.acme.einvoice.application.artifact;

import java.time.Instant;
import java.util.Map;

public record Artifact(
        ArtifactReference reference,
        byte[] payload,
        Map<String, String> metadata,
        Instant createdAt
) {
}
