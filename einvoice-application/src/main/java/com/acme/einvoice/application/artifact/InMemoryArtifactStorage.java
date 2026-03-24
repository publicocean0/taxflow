package com.acme.einvoice.application.artifact;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryArtifactStorage implements ArtifactStorage {
    private final Map<String, Artifact> artifacts = new ConcurrentHashMap<>();

    @Override
    public ArtifactReference store(Artifact artifact) {
        String id = artifact.reference() == null || artifact.reference().id() == null
                ? UUID.randomUUID().toString()
                : artifact.reference().id();

        ArtifactReference reference = new ArtifactReference(id, artifact.reference() == null
                ? "application/octet-stream"
                : artifact.reference().mediaType());

        artifacts.put(id, new Artifact(reference, artifact.payload(), artifact.metadata(), artifact.createdAt()));
        return reference;
    }

    @Override
    public Optional<Artifact> get(ArtifactReference reference) {
        return Optional.ofNullable(artifacts.get(reference.id()));
    }
}
