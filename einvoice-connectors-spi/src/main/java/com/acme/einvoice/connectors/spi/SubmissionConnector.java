package com.acme.einvoice.connectors.spi;

import java.util.Optional;

public interface SubmissionConnector {
    ConnectorId id();
    String type();
    SubmissionResult submit(SubmissionCommand command);

    default Optional<ExternalStatusResult> fetchStatus(StatusQueryCommand command) {
        return Optional.empty();
    }
}
