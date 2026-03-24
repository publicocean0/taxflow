package com.acme.einvoice.application.exception;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.common.model.TenantId;

public final class DuplicateSubmissionException extends RuntimeException {
    public DuplicateSubmissionException(TenantId tenantId, ServiceId serviceId, String documentId, String idempotencyKey) {
        super("Duplicate submission for tenant=" + tenantId.value() + ", service=" + serviceId.value() + ", document=" + documentId + ", key=" + idempotencyKey);
    }
}
