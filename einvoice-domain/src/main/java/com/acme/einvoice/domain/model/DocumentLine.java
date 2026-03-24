package com.acme.einvoice.domain.model;

import java.math.BigDecimal;

public interface DocumentLine {
    String id();
    String description();
    BigDecimal quantity();
    BigDecimal unitPrice();
    TaxCategory taxCategory();
}
