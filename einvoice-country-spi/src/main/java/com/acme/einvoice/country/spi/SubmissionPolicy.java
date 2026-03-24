package com.acme.einvoice.country.spi;

public interface SubmissionPolicy {
    boolean requiresSignature();
    String connectorType();
}
