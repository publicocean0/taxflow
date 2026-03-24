package com.acme.einvoice.application.tenant;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.connectors.spi.ConnectorId;

public record ConnectorBinding(CountryCode countryCode, ConnectorId connectorId) {
}
