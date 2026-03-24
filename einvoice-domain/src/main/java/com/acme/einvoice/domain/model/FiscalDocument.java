package com.acme.einvoice.domain.model;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.ServiceId;

import java.time.Instant;
import java.util.List;

public interface FiscalDocument {
    String id();
    ServiceId serviceId();
    CountryCode countryCode();
    String documentType();
    Party seller();
    Party buyer();
    List<DocumentLine> lines();
    Instant issuedAt();
    long version();
}
