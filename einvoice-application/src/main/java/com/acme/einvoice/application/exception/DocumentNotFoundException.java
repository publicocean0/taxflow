package com.acme.einvoice.application.exception;

public final class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String documentId) {
        super("Document not found: " + documentId);
    }
}
