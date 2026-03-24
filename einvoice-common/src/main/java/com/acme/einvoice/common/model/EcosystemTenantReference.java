package com.acme.einvoice.common.model;

import java.util.Objects;

public record EcosystemTenantReference(TenantId tenantId, String displayName) {
    public EcosystemTenantReference {
        Objects.requireNonNull(tenantId, "tenantId is required");
    }
}
