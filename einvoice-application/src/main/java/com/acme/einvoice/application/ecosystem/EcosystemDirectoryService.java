package com.acme.einvoice.application.ecosystem;

import com.acme.einvoice.common.model.EcosystemServiceReference;
import com.acme.einvoice.common.model.EcosystemTenantReference;
import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.common.model.TenantId;

import java.util.List;
import java.util.Optional;

public interface EcosystemDirectoryService {
    Optional<EcosystemTenantReference> getTenant(TenantId tenantId);

    Optional<EcosystemServiceReference> getService(ServiceId serviceId);

    List<EcosystemServiceReference> listServices(TenantId tenantId);
}
