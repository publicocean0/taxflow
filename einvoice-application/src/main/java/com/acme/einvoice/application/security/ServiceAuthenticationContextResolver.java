package com.acme.einvoice.application.security;

public interface ServiceAuthenticationContextResolver {
    ServiceAuthenticationContext resolve(String bearerToken);
}
