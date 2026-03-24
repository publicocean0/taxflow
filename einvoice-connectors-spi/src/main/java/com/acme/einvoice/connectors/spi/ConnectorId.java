package com.acme.einvoice.connectors.spi;

import java.util.Objects;

public record ConnectorId(String value) {
    public ConnectorId {
        Objects.requireNonNull(value, "value is required");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value cannot be blank");
        }
    }

    public static ConnectorId of(String value) {
        return new ConnectorId(value);
    }
}
