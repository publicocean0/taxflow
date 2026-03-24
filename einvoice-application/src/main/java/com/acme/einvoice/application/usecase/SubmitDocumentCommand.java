package com.acme.einvoice.application.usecase;

import com.acme.einvoice.common.model.TenantId;

public record SubmitDocumentCommand(TenantId tenantId, String documentId) {
}
