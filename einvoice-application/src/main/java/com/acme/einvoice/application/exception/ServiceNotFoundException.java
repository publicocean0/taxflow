package com.acme.einvoice.application.exception;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.common.model.TenantId;

public final class ServiceNotFoundException extends RuntimeException {
    public ServiceNotFoundException(TenantId tenantId, ServiceId serviceId) {
        super("Service not found in ecosystem directory for service=" + serviceId.value()
                + (tenantId == null ? "" : (", tenant=" + tenantId.value())));
    }
}
