package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.domain.model.TransmissionStatusUpdate;
import com.acme.einvoice.domain.repository.TransmissionStatusUpdateRepository;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryTransmissionStatusUpdateRepository implements TransmissionStatusUpdateRepository {
    private final ConcurrentMap<String, TransmissionStatusUpdate> updates = new ConcurrentHashMap<>();

    @Override
    public TransmissionStatusUpdate save(TransmissionStatusUpdate statusUpdate) {
        updates.put(statusUpdate.id(), statusUpdate);
        return statusUpdate;
    }
}
