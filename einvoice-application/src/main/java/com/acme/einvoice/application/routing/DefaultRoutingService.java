package com.acme.einvoice.application.routing;

import com.acme.einvoice.application.exception.ConnectorNotFoundException;
import com.acme.einvoice.application.exception.CountryModuleNotFoundException;
import com.acme.einvoice.application.exception.TenantConfigurationNotFoundException;
import com.acme.einvoice.application.tenant.TenantConfigurationService;
import com.acme.einvoice.application.tenant.TenantFiscalConfiguration;
import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.connectors.spi.ConnectorId;
import com.acme.einvoice.connectors.spi.ConnectorRegistry;
import com.acme.einvoice.connectors.spi.SubmissionConnector;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.CountryModuleRegistry;

public final class DefaultRoutingService implements RoutingService {
    private final CountryModuleRegistry countryModuleRegistry;
    private final ConnectorRegistry connectorRegistry;
    private final TenantConfigurationService tenantConfigurationService;

    public DefaultRoutingService(
            CountryModuleRegistry countryModuleRegistry,
            ConnectorRegistry connectorRegistry,
            TenantConfigurationService tenantConfigurationService
    ) {
        this.countryModuleRegistry = countryModuleRegistry;
        this.connectorRegistry = connectorRegistry;
        this.tenantConfigurationService = tenantConfigurationService;
    }

    @Override
    public CountryModule resolveCountryModule(CountryCode countryCode) {
        CountryModule module = countryModuleRegistry.get(countryCode);
        if (module == null) {
            throw new CountryModuleNotFoundException(countryCode);
        }
        return module;
    }

    @Override
    public SubmissionConnector resolveConnector(TenantId tenantId, CountryCode countryCode) {
        TenantFiscalConfiguration configuration = tenantConfigurationService.getByTenantId(tenantId)
                .orElseThrow(() -> new TenantConfigurationNotFoundException(tenantId));

        if (!configuration.isCountryEnabled(countryCode)) {
            throw new TenantConfigurationNotFoundException(tenantId, countryCode);
        }

        ConnectorId connectorId = configuration.connectorFor(countryCode)
                .orElseThrow(() -> new TenantConfigurationNotFoundException(tenantId, countryCode));

        SubmissionConnector connector = connectorRegistry.get(connectorId);
        if (connector == null) {
            throw new ConnectorNotFoundException(connectorId);
        }

        return connector;
    }
}
