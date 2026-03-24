package com.acme.einvoice.common.model;

import java.util.Objects;

public record EcosystemServiceReference(TenantId tenantId, ServiceId serviceId, String serviceName) {
    public EcosystemServiceReference {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(serviceId, "serviceId is required");
    }
}
