package com.acme.einvoice.domain.repository;

import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.domain.model.FiscalDocument;

import java.util.List;
import java.util.Optional;

public interface FiscalDocumentRepository {
    FiscalDocument save(FiscalDocument document);
    Optional<FiscalDocument> findById(String documentId);
    List<FiscalDocument> findByTenantId(TenantId tenantId);
}
