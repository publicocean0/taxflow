package com.acme.einvoice.connector.sdi;

import com.acme.einvoice.connectors.spi.ConnectorId;
import com.acme.einvoice.connectors.spi.SubmissionCommand;
import com.acme.einvoice.connectors.spi.SubmissionConnector;
import com.acme.einvoice.connectors.spi.SubmissionResult;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class SdiSubmissionConnector implements SubmissionConnector {
    @Override
    public ConnectorId id() {
        return ConnectorId.of("SDI_DIRECT");
    }

    @Override
    public String type() {
        return "SDI";
    }

    @Override
    public SubmissionResult submit(SubmissionCommand command) {
        return new SubmissionResult(UUID.randomUUID().toString(), "SUBMITTED", Optional.empty(), Instant.now());
    }
}
