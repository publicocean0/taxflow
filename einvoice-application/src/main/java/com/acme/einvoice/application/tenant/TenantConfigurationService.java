package com.acme.einvoice.application.tenant;

import com.acme.einvoice.common.model.TenantId;

import java.util.Optional;

public interface TenantConfigurationService {
    Optional<TenantFiscalConfiguration> getByTenantId(TenantId tenantId);
}
