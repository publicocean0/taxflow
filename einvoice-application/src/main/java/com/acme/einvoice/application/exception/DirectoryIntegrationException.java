package com.acme.einvoice.application.exception;

public final class DirectoryIntegrationException extends RuntimeException {
    public DirectoryIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
