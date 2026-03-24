package com.acme.einvoice.country.it;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.country.spi.CountryModule;
import com.acme.einvoice.country.spi.CountryValidator;
import com.acme.einvoice.country.spi.DocumentRenderer;
import com.acme.einvoice.country.spi.StatusTranslator;
import com.acme.einvoice.country.spi.SubmissionPolicy;

public final class ItalyCountryModule implements CountryModule {

    private final CountryValidator validator;
    private final DocumentRenderer renderer;
    private final SubmissionPolicy submissionPolicy;
    private final StatusTranslator statusTranslator;

    public ItalyCountryModule(
            CountryValidator validator,
            DocumentRenderer renderer,
            SubmissionPolicy submissionPolicy,
            StatusTranslator statusTranslator
    ) {
        this.validator = validator;
        this.renderer = renderer;
        this.submissionPolicy = submissionPolicy;
        this.statusTranslator = statusTranslator;
    }

    @Override
    public CountryCode supportedCountry() {
        return CountryCode.of("IT");
    }

    @Override
    public CountryValidator validator() {
        return validator;
    }

    @Override
    public DocumentRenderer renderer() {
        return renderer;
    }

    @Override
    public SubmissionPolicy submissionPolicy() {
        return submissionPolicy;
    }

    @Override
    public StatusTranslator statusTranslator() {
        return statusTranslator;
    }
}
