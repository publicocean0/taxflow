package com.acme.einvoice.connectors.spi;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.country.spi.RenderedDocument;

public record SubmissionCommand(
        ServiceId serviceId,
        CountryCode countryCode,
        String documentId,
        RenderedDocument renderedDocument
) {
}
