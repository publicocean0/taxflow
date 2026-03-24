package com.acme.einvoice.domain.model;

public interface Party {
    String id();
    String role();
    String legalName();
    String vatNumber();
}
