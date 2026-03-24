package com.acme.einvoice.connectors.spi;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.country.spi.RenderedDocument;

public record SubmissionCommand(
        TenantId tenantId,
        CountryCode countryCode,
        String documentId,
        RenderedDocument renderedDocument
) {
}
