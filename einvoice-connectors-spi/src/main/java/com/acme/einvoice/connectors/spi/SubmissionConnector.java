package com.acme.einvoice.connectors.spi;

public interface SubmissionConnector {
    ConnectorId id();
    String type();
    SubmissionResult submit(SubmissionCommand command);
}
