package com.acme.einvoice.country.spi;

import java.util.List;

public record ValidationReport(boolean valid, List<String> errors) {
    public static ValidationReport success() {
        return new ValidationReport(true, List.of());
    }
}
