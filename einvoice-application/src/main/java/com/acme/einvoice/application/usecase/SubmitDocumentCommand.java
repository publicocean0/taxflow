package com.acme.einvoice.application.usecase;

import com.acme.einvoice.common.model.ServiceId;

import java.util.Optional;

public record SubmitDocumentCommand(ServiceId serviceId, String documentId, Optional<String> idempotencyKey) {
    public SubmitDocumentCommand(ServiceId serviceId, String documentId) {
        this(serviceId, documentId, Optional.empty());
    }
}
