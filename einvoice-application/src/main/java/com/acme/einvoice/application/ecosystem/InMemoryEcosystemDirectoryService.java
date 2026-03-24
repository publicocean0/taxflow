package com.acme.einvoice.application.ecosystem;

import com.acme.einvoice.common.model.EcosystemServiceReference;
import com.acme.einvoice.common.model.EcosystemTenantReference;
import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.common.model.TenantId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryEcosystemDirectoryService implements EcosystemDirectoryService {
    private final Map<TenantId, EcosystemTenantReference> tenants;
    private final Map<TenantId, List<EcosystemServiceReference>> servicesByTenant;
    private final Map<ServiceId, EcosystemServiceReference> servicesById;

    public InMemoryEcosystemDirectoryService(
            Map<TenantId, EcosystemTenantReference> tenants,
            Map<TenantId, List<EcosystemServiceReference>> servicesByTenant
    ) {
        this.tenants = Map.copyOf(tenants);
        this.servicesByTenant = Map.copyOf(servicesByTenant);
        this.servicesById = servicesByTenant.values().stream()
                .flatMap(List::stream)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(EcosystemServiceReference::serviceId, s -> s));
    }

    @Override
    public Optional<EcosystemTenantReference> getTenant(TenantId tenantId) {
        return Optional.ofNullable(tenants.get(tenantId));
    }

    @Override
    public Optional<EcosystemServiceReference> getService(ServiceId serviceId) {
        return Optional.ofNullable(servicesById.get(serviceId));
    }

    @Override
    public List<EcosystemServiceReference> listServices(TenantId tenantId) {
        return servicesByTenant.getOrDefault(tenantId, List.of());
    }
}
