package com.acme.einvoice.bootstrap;

import com.acme.einvoice.common.model.TenantId;
import com.acme.einvoice.domain.model.FiscalDocument;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;

import java.util.List;
import java.util.Optional;

final class InMemoryBootstrapDocumentRepository implements FiscalDocumentRepository {
    @Override
    public FiscalDocument save(FiscalDocument document) {
        return document;
    }

    @Override
    public Optional<FiscalDocument> findById(String documentId) {
        return Optional.empty();
    }

    @Override
    public List<FiscalDocument> findByTenantId(TenantId tenantId) {
        return List.of();
    }
}
