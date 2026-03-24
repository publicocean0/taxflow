package com.acme.einvoice.application.service;

import com.acme.einvoice.common.model.ServiceId;

import java.util.UUID;

record SubmissionRequestedEventPayload(String transmissionId, ServiceId serviceId, String documentId) {
    String serialize() {
        return transmissionId + "|" + serviceId.value() + "|" + documentId;
    }

    static SubmissionRequestedEventPayload deserialize(String payload) {
        String[] parts = payload.split("\\|", 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Unsupported submission payload: " + payload);
        }
        return new SubmissionRequestedEventPayload(parts[0], new ServiceId(UUID.fromString(parts[1])), parts[2]);
    }
}
