package com.acme.einvoice.domain.model;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.TenantId;

import java.time.Instant;
import java.util.List;

public interface FiscalDocument {
    String id();
    TenantId tenantId();
    CountryCode countryCode();
    String documentType();
    Party seller();
    Party buyer();
    List<DocumentLine> lines();
    Instant issuedAt();
}
