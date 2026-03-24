package com.acme.einvoice.application.exception;

import com.acme.einvoice.common.model.CountryCode;

public final class CountryModuleNotFoundException extends RuntimeException {
    public CountryModuleNotFoundException(CountryCode countryCode) {
        super("Country module not found for country: " + countryCode.value());
    }
}
