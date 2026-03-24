package com.acme.einvoice.common.model;

import java.util.UUID;

public record TenantId(UUID value) {
    public static TenantId random() {
        return new TenantId(UUID.randomUUID());
    }
}
