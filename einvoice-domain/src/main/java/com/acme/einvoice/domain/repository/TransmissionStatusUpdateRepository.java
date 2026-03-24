package com.acme.einvoice.domain.repository;

import com.acme.einvoice.domain.model.TransmissionStatusUpdate;

import java.util.List;

public interface TransmissionStatusUpdateRepository {
    TransmissionStatusUpdate save(TransmissionStatusUpdate statusUpdate);

    List<TransmissionStatusUpdate> findByTransmissionId(String transmissionId);
}
