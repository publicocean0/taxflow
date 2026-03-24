package com.acme.einvoice.domain.model;

public enum TransmissionStatus {
    PENDING_SUBMISSION,
    SUBMITTING,
    SUBMITTED,
    STATUS_PENDING,
    ACCEPTED,
    REJECTED,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    CANCELLED;

    public boolean isTerminal() {
        return this == ACCEPTED || this == REJECTED || this == FAILED_FINAL || this == CANCELLED;
    }
}
