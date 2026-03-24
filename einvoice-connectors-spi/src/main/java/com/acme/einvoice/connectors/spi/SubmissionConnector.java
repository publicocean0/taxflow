package com.acme.einvoice.connectors.spi;

public interface SubmissionConnector {
    String id();
    String type();
    SubmissionResult submit(SubmissionCommand command);
}
