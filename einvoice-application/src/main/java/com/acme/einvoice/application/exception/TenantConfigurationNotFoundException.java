package com.acme.einvoice.application.exception;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.TenantId;

public final class TenantConfigurationNotFoundException extends RuntimeException {
    public TenantConfigurationNotFoundException(TenantId tenantId) {
        super("Tenant fiscal configuration not found for tenant: " + tenantId.value());
    }

    public TenantConfigurationNotFoundException(TenantId tenantId, CountryCode countryCode) {
        super("Tenant " + tenantId.value() + " has no fiscal configuration for country " + countryCode.value());
    }
}
