package com.acme.einvoice.country.spi;

import java.util.Map;

public record RenderedDocument(String format, byte[] payload, Map<String, String> metadata) {
}
