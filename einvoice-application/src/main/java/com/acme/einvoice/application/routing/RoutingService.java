package com.acme.einvoice.application.routing;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.connectors.spi.SubmissionConnector;
import com.acme.einvoice.country.spi.CountryModule;

public interface RoutingService {
    CountryModule resolveCountryModule(CountryCode countryCode);
    SubmissionConnector resolveConnector(TenantId tenantId, CountryCode countryCode);
}
