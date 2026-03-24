package com.acme.einvoice.domain.model;

import com.acme.einvoice.common.model.ServiceId;

import java.time.Instant;
import java.util.Map;

public record DocumentArtifactMetadata(
        String id,
        ServiceId serviceId,
        String documentId,
        String artifactType,
        String format,
        String storageUri,
        String checksum,
        Map<String, String> metadata,
        Instant createdAt
) {
}
