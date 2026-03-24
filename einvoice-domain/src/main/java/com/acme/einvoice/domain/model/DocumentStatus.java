package com.acme.einvoice.domain.model;

public enum DocumentStatus {
    DRAFT,
    VALIDATED,
    RENDERED,
    SIGNED,
    SUBMITTED,
    ACCEPTED,
    REJECTED,
    DELIVERED,
    FAILED,
    CANCELLED
}
