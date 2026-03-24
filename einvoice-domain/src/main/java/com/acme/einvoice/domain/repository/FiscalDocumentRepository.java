package com.acme.einvoice.domain.repository;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.FiscalDocument;

import java.util.List;
import java.util.Optional;

public interface FiscalDocumentRepository {
    FiscalDocument save(FiscalDocument document);

    FiscalDocument update(FiscalDocument document, long expectedVersion);

    Optional<FiscalDocument> findById(ServiceId serviceId, String documentId);

    List<FiscalDocument> findByServiceId(ServiceId serviceId);
}
