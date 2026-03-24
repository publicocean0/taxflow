package com.acme.einvoice.application.exception;

public final class SubmissionFailedException extends RuntimeException {
    public SubmissionFailedException(String message) {
        super(message);
    }

    public SubmissionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
