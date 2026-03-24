package com.acme.einvoice.bootstrap;

import com.acme.einvoice.common.model.ServiceId;
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
    public FiscalDocument update(FiscalDocument document, long expectedVersion) {
        return document;
    }

    @Override
    public Optional<FiscalDocument> findById(ServiceId serviceId, String documentId) {
        return Optional.empty();
    }

    @Override
    public List<FiscalDocument> findByServiceId(ServiceId serviceId) {
        return List.of();
    }
}
