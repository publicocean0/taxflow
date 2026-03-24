package com.acme.einvoice.application.exception;

import com.acme.einvoice.connectors.spi.ConnectorId;

public final class ConnectorNotFoundException extends RuntimeException {
    public ConnectorNotFoundException(ConnectorId connectorId) {
        super("Connector not found: " + connectorId.value());
    }
}
