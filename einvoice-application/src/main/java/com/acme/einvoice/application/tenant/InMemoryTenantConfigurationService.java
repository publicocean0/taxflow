package com.acme.einvoice.application.tenant;

import com.acme.einvoice.common.model.ServiceId;

import java.util.Map;
import java.util.Optional;

public final class InMemoryTenantConfigurationService implements TenantConfigurationService {
    private final Map<ServiceId, TenantFiscalConfiguration> configurations;

    public InMemoryTenantConfigurationService(Map<ServiceId, TenantFiscalConfiguration> initialConfigurations) {
        this.configurations = Map.copyOf(initialConfigurations);
    }

    @Override
    public Optional<TenantFiscalConfiguration> getByServiceId(ServiceId serviceId) {
        return Optional.ofNullable(configurations.get(serviceId));
    }
}
