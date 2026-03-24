package com.acme.einvoice.application.tenant;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.connectors.spi.ConnectorId;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record TenantFiscalConfiguration(
        TenantId tenantId,
        Set<CountryCode> enabledCountries,
        Map<CountryCode, ConnectorBinding> connectorBindings,
        EnvironmentProfile environmentProfile,
        SignaturePolicy signaturePolicy,
        ArchivePolicy archivePolicy
) {
    public TenantFiscalConfiguration {
        enabledCountries = Set.copyOf(enabledCountries);
        connectorBindings = Map.copyOf(connectorBindings);
    }

    public Optional<ConnectorId> connectorFor(CountryCode countryCode) {
        return Optional.ofNullable(connectorBindings.get(countryCode)).map(ConnectorBinding::connectorId);
    }

    public boolean isCountryEnabled(CountryCode countryCode) {
        return enabledCountries.contains(countryCode);
    }
}
