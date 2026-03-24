package com.acme.einvoice.application.service;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.ReconciliationOutcome;

public interface IngestExternalStatusUpdateUseCase {
    ReconciliationOutcome ingest(IngestExternalStatusUpdateCommand command);

    record IngestExternalStatusUpdateCommand(
            ServiceId serviceId,
            String transmissionId,
            String sourceType,
            String deduplicationKey,
            String externalStatusCode,
            String externalStatusLabel,
            String externalReference,
            String rawPayload
    ) {
    }
}
