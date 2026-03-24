package com.acme.einvoice.domain.model;

import java.util.Optional;

public interface Invoice extends FiscalDocument {
    String invoiceNumber();
    Optional<String> previousReference();
}
