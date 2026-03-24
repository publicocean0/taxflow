package com.acme.einvoice.country.spi;

import com.acme.einvoice.common.model.CountryCode;

import java.util.Collection;

public interface CountryModuleRegistry {
    CountryModule get(CountryCode countryCode);
    Collection<CountryModule> all();
}
