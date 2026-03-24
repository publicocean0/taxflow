package com.acme.einvoice.application.exception;

public final class TransmissionNotFoundException extends RuntimeException {
    public TransmissionNotFoundException(String transmissionId) {
        super("Transmission not found: " + transmissionId);
    }
}
