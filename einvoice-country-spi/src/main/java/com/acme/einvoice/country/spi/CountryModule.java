package com.acme.einvoice.country.spi;

import com.acme.einvoice.common.model.CountryCode;

public interface CountryModule {
    CountryCode supportedCountry();
    CountryValidator validator();
    DocumentRenderer renderer();
    SubmissionPolicy submissionPolicy();
    StatusTranslator statusTranslator();
}
