package com.acme.einvoice.domain.model;

import java.math.BigDecimal;
import java.util.Optional;

public interface TaxCategory {
    String code();
    BigDecimal rate();
    Optional<String> exemptionReasonCode();
}
