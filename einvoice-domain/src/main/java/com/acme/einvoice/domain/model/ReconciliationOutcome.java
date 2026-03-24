package com.acme.einvoice.domain.model;

public enum ReconciliationOutcome {
    APPLIED,
    DUPLICATE,
    IGNORED_OUTDATED,
    IGNORED_TERMINAL,
    TRANSMISSION_NOT_FOUND
}
