package com.acme.einvoice.domain.repository;

import com.acme.einvoice.domain.model.TransmissionStatusUpdate;

public interface TransmissionStatusUpdateRepository {
    TransmissionStatusUpdate save(TransmissionStatusUpdate statusUpdate);
}
