package com.acme.einvoice.country.spi;

import com.acme.einvoice.domain.model.DocumentStatus;

public interface StatusTranslator {
    DocumentStatus translate(String externalStatus);
}
