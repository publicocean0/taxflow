package com.acme.einvoice.application.tenant;

import com.acme.einvoice.common.model.TenantId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryTenantConfigurationService implements TenantConfigurationService {
    private final Map<TenantId, TenantFiscalConfiguration> configurations;

    public InMemoryTenantConfigurationService(Map<TenantId, TenantFiscalConfiguration> initialConfigurations) {
        this.configurations = new ConcurrentHashMap<>(initialConfigurations);
    }

    @Override
    public Optional<TenantFiscalConfiguration> getByTenantId(TenantId tenantId) {
        return Optional.ofNullable(configurations.get(tenantId));
    }
}
