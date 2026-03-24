package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.FiscalDocument;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryFiscalDocumentRepository implements FiscalDocumentRepository {
    private final Map<ScopedDocumentKey, FiscalDocument> documents = new ConcurrentHashMap<>();

    @Override
    public FiscalDocument save(FiscalDocument document) {
        documents.put(new ScopedDocumentKey(document.serviceId(), document.id()), document);
        return document;
    }

    @Override
    public FiscalDocument update(FiscalDocument document, long expectedVersion) {
        ScopedDocumentKey key = new ScopedDocumentKey(document.serviceId(), document.id());
        FiscalDocument existing = documents.get(key);
        if (existing == null || existing.version() != expectedVersion) {
            throw new IllegalStateException("Optimistic concurrency conflict on fiscal_document id=" + document.id() + " expectedVersion=" + expectedVersion);
        }
        documents.put(key, document);
        return document;
    }

    @Override
    public Optional<FiscalDocument> findById(ServiceId serviceId, String documentId) {
        return Optional.ofNullable(documents.get(new ScopedDocumentKey(serviceId, documentId)));
    }

    @Override
    public List<FiscalDocument> findByServiceId(ServiceId serviceId) {
        return documents.entrySet().stream()
                .filter(e -> e.getKey().serviceId().equals(serviceId))
                .map(Map.Entry::getValue)
                .toList();
    }

    private record ScopedDocumentKey(ServiceId serviceId, String documentId) {
    }
}
