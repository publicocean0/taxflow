package com.acme.einvoice.application.security;

public interface AccessTokenVerifier {
    TokenVerificationResult verify(String bearerToken);
}
