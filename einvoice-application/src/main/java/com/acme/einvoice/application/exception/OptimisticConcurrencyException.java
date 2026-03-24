package com.acme.einvoice.application.exception;

public final class OptimisticConcurrencyException extends RuntimeException {
    public OptimisticConcurrencyException(String aggregate, String id, long expectedVersion) {
        super("Optimistic concurrency conflict on " + aggregate + " id=" + id + " expectedVersion=" + expectedVersion);
    }
}
