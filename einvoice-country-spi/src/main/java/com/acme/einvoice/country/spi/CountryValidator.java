package com.acme.einvoice.country.spi;

import com.acme.einvoice.domain.model.FiscalDocument;

public interface CountryValidator {
    ValidationReport validate(FiscalDocument document, CountryContext context);
}
