package com.acme.einvoice.country.spi;

import com.acme.einvoice.common.model.ServiceId;

public record CountryContext(ServiceId serviceId) {
    public static CountryContext of(ServiceId serviceId) {
        return new CountryContext(serviceId);
    }
}
