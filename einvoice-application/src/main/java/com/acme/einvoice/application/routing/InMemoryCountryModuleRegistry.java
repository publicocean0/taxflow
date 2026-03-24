package com.acme.einvoice.application.routing;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.CountryModuleRegistry;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryCountryModuleRegistry implements CountryModuleRegistry {
    private final Map<CountryCode, CountryModule> modules;

    public InMemoryCountryModuleRegistry(Collection<CountryModule> modules) {
        this.modules = new ConcurrentHashMap<>();
        modules.forEach(module -> this.modules.put(module.supportedCountry(), module));
    }

    @Override
    public CountryModule get(CountryCode countryCode) {
        return modules.get(countryCode);
    }

    @Override
    public Collection<CountryModule> all() {
        return modules.values();
    }
}
