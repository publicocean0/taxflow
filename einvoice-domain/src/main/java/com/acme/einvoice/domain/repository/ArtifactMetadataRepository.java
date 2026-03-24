package com.acme.einvoice.domain.repository;

import com.acme.einvoice.domain.model.DocumentArtifactMetadata;

import java.util.Optional;

public interface ArtifactMetadataRepository {
    DocumentArtifactMetadata save(DocumentArtifactMetadata artifactMetadata);

    Optional<DocumentArtifactMetadata> findById(String id);
}
