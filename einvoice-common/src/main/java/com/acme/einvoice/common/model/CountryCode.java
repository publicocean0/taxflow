package com.acme.einvoice.common.model;

import java.util.Objects;

public record CountryCode(String value) {
    public CountryCode {
        Objects.requireNonNull(value, "value is required");
        if (value.length() != 2) {
            throw new IllegalArgumentException("Country code must have length 2");
        }
    }

    public static CountryCode of(String value) {
        return new CountryCode(value.toUpperCase());
    }
}
