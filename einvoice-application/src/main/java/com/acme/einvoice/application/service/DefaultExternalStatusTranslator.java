package com.acme.einvoice.application.service;

import com.acme.einvoice.domain.model.NormalizedTransmissionStatusUpdate;
import com.acme.einvoice.domain.model.TransmissionExternalEvent;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;

import java.util.Locale;
import java.util.Optional;

public final class DefaultExternalStatusTranslator implements ExternalStatusTranslator {
    @Override
    public NormalizedTransmissionStatusUpdate translate(TransmissionRecord transmission, TransmissionExternalEvent externalEvent) {
        String status = externalEvent.externalStatusCode().orElse("UNKNOWN").toUpperCase(Locale.ROOT);
        return switch (status) {
            case "ACCEPTED", "DELIVERED", "SUCCESS" -> new NormalizedTransmissionStatusUpdate(
                    TransmissionStatus.ACCEPTED,
                    Optional.of(status),
                    externalEvent.occurredAtExternal(),
                    true,
                    "Mapped as accepted by generic translator"
            );
            case "REJECTED", "ERROR", "FAILED" -> new NormalizedTransmissionStatusUpdate(
                    TransmissionStatus.REJECTED,
                    Optional.of(status),
                    externalEvent.occurredAtExternal(),
                    true,
                    "Mapped as rejected by generic translator"
            );
            case "PENDING", "IN_PROGRESS", "PROCESSING" -> new NormalizedTransmissionStatusUpdate(
                    TransmissionStatus.STATUS_PENDING,
                    Optional.of(status),
                    externalEvent.occurredAtExternal(),
                    false,
                    "Mapped as pending by generic translator"
            );
            default -> new NormalizedTransmissionStatusUpdate(
                    transmission.status().isTerminal() ? transmission.status() : TransmissionStatus.STATUS_PENDING,
                    Optional.of(status),
                    externalEvent.occurredAtExternal(),
                    transmission.status().isTerminal(),
                    "Unknown external status; holding current lifecycle"
            );
        };
    }
}
