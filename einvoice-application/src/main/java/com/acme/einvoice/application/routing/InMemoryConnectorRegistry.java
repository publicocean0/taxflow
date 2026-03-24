package com.acme.einvoice.application.routing;

import com.acme.einvoice.connectors.spi.ConnectorId;
import com.acme.einvoice.connectors.spi.ConnectorRegistry;
import com.acme.einvoice.connectors.spi.SubmissionConnector;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryConnectorRegistry implements ConnectorRegistry {
    private final Map<ConnectorId, SubmissionConnector> connectors;

    public InMemoryConnectorRegistry(Collection<SubmissionConnector> connectors) {
        this.connectors = new ConcurrentHashMap<>();
        connectors.forEach(connector -> this.connectors.put(connector.id(), connector));
    }

    @Override
    public SubmissionConnector get(ConnectorId connectorId) {
        return connectors.get(connectorId);
    }

    @Override
    public Collection<SubmissionConnector> all() {
        return connectors.values();
    }
}
