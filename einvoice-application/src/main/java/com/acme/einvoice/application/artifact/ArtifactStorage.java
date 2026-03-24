package com.acme.einvoice.application.artifact;

import java.util.Optional;

public interface ArtifactStorage {
    ArtifactReference store(Artifact artifact);
    Optional<Artifact> get(ArtifactReference reference);
}
