package com.acme.einvoice.application.exception;

import com.acme.einvoice.country.spi.ValidationReport;

public final class ValidationFailedException extends RuntimeException {
    public ValidationFailedException(String documentId, ValidationReport report) {
        super("Validation failed for document " + documentId + ": " + report.errors());
    }
}
