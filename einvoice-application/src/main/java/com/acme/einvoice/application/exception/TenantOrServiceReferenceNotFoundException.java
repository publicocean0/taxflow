package com.acme.einvoice.application.exception;

public final class TenantOrServiceReferenceNotFoundException extends RuntimeException {
    public TenantOrServiceReferenceNotFoundException(String message) {
        super(message);
    }
}
