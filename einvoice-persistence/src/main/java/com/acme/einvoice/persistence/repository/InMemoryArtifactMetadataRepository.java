package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.domain.model.DocumentArtifactMetadata;
import com.acme.einvoice.domain.repository.ArtifactMetadataRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryArtifactMetadataRepository implements ArtifactMetadataRepository {
    private final Map<String, DocumentArtifactMetadata> artifacts = new ConcurrentHashMap<>();

    @Override
    public DocumentArtifactMetadata save(DocumentArtifactMetadata artifactMetadata) {
        artifacts.put(artifactMetadata.id(), artifactMetadata);
        return artifactMetadata;
    }

    @Override
    public Optional<DocumentArtifactMetadata> findById(String id) {
        return Optional.ofNullable(artifacts.get(id));
    }
}
