package com.acme.einvoice.country.spi;

import java.util.Optional;

public record ValidationMessage(
        String code,
        String message,
        Optional<String> path,
        ValidationSeverity severity
) {
    public ValidationMessage {
        path = path == null ? Optional.empty() : path;
    }

    public static ValidationMessage error(String code, String message, String path) {
        return new ValidationMessage(code, message, Optional.ofNullable(path), ValidationSeverity.ERROR);
    }

    public static ValidationMessage warning(String code, String message, String path) {
        return new ValidationMessage(code, message, Optional.ofNullable(path), ValidationSeverity.WARNING);
    }
}
