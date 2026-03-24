package com.acme.einvoice.domain.repository;

import com.acme.einvoice.domain.model.OutboxEvent;
import com.acme.einvoice.domain.model.TransmissionRecord;

public interface SubmissionIntentRepository {
    void saveIntent(TransmissionRecord transmissionRecord, OutboxEvent outboxEvent);
}
