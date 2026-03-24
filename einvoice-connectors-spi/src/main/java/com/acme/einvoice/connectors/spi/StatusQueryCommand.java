package com.acme.einvoice.connectors.spi;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.ServiceId;

import java.util.Optional;

public record StatusQueryCommand(
        ServiceId serviceId,
        CountryCode countryCode,
        String transmissionId,
        Optional<String> externalReference
) {
}
