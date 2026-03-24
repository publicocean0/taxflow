package com.acme.einvoice.country.spi;

import com.acme.einvoice.domain.model.FiscalDocument;

public interface DocumentRenderer {
    RenderedDocument render(FiscalDocument document, CountryContext context);
}
