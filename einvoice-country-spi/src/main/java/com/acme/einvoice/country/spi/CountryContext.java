package com.acme.einvoice.country.spi;

import com.acme.einvoice.common.model.TenantId;

public record CountryContext(TenantId tenantId) {
    public static CountryContext of(TenantId tenantId) {
        return new CountryContext(tenantId);
    }
}
