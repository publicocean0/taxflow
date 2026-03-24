package com.acme.einvoice.application.exception;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.ServiceId;

public final class TenantConfigurationNotFoundException extends RuntimeException {
    public TenantConfigurationNotFoundException(ServiceId serviceId) {
        super("Service fiscal configuration not found for service: " + serviceId.value());
    }

    public TenantConfigurationNotFoundException(ServiceId serviceId, CountryCode countryCode) {
        super("Service " + serviceId.value() + " has no fiscal configuration for country " + countryCode.value());
    }
}
