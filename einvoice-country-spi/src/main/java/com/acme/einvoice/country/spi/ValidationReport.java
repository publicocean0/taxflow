package com.acme.einvoice.country.spi;

import java.util.List;

public record ValidationReport(List<ValidationMessage> messages) {
    public ValidationReport {
        messages = List.copyOf(messages);
    }

    public static ValidationReport valid() {
        return new ValidationReport(List.of());
    }

    public static ValidationReport of(List<ValidationMessage> messages) {
        return new ValidationReport(messages);
    }

    public boolean isValid() {
        return messages.stream().noneMatch(message -> message.severity() == ValidationSeverity.ERROR);
    }

    public List<ValidationMessage> errors() {
        return messages.stream().filter(message -> message.severity() == ValidationSeverity.ERROR).toList();
    }

    public List<ValidationMessage> warnings() {
        return messages.stream().filter(message -> message.severity() == ValidationSeverity.WARNING).toList();
    }
}
