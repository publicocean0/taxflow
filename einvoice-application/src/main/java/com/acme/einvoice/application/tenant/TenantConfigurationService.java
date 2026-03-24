package com.acme.einvoice.application.tenant;

import com.acme.einvoice.common.model.ServiceId;

import java.util.Optional;

public interface TenantConfigurationService {
    Optional<TenantFiscalConfiguration> getByServiceId(ServiceId serviceId);
}
