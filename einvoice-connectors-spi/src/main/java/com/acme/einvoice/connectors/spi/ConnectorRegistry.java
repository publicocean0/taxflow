package com.acme.einvoice.connectors.spi;

import java.util.Collection;

public interface ConnectorRegistry {
    SubmissionConnector get(String connectorId);
    Collection<SubmissionConnector> all();
}
