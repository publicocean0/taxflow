package com.acme.einvoice.connectors.spi;

import java.util.Collection;

public interface ConnectorRegistry {
    SubmissionConnector get(ConnectorId connectorId);
    Collection<SubmissionConnector> all();
}
