package com.acme.einvoice.common.model;

import java.util.UUID;

public record ServiceId(UUID value) {
    public static ServiceId random() {
        return new ServiceId(UUID.randomUUID());
    }
}
