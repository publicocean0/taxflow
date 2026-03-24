package com.acme.einvoice.persistence.repository;

import com.acme.einvoice.common.model.CountryCode;
import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.domain.model.DocumentLine;
import com.acme.einvoice.domain.model.FiscalDocument;
import com.acme.einvoice.domain.model.OutboxEvent;
import com.acme.einvoice.domain.model.OutboxEventStatus;
import com.acme.einvoice.domain.model.Party;
import com.acme.einvoice.domain.model.TaxCategory;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.model.TransmissionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryRepositoriesTest {

    @Test
    void fiscalDocument_saveAndScopedLoad() {
        InMemoryFiscalDocumentRepository repository = new InMemoryFiscalDocumentRepository();
        TestFiscalDocument document = new TestFiscalDocument("3fa85f64-5717-4562-b3fc-2c963f66afa6", ServiceId.random(), 0);

        repository.save(document);

        assertEquals(document.id(), repository.findById(document.serviceId(), document.id()).orElseThrow().id());
    }

    @Test
    void fiscalDocument_optimisticConflict() {
        InMemoryFiscalDocumentRepository repository = new InMemoryFiscalDocumentRepository();
        TestFiscalDocument document = new TestFiscalDocument("3fa85f64-5717-4562-b3fc-2c963f66afa7", ServiceId.random(), 0);
        repository.save(document);

        assertThrows(IllegalStateException.class,
                () -> repository.update(new TestFiscalDocument(document.id(), document.serviceId(), 1), 99));
    }

    @Test
    void transmission_uniqueIdempotencyInServiceDocumentScope() {
        InMemoryTransmissionRepository repository = new InMemoryTransmissionRepository();
        ServiceId serviceId = ServiceId.random();

        TransmissionRecord record = new TransmissionRecord(
                "92e6de8b-53a8-4a9f-9631-fcdaf8f24cf0",
                serviceId,
                "3fa85f64-5717-4562-b3fc-2c963f66afa8",
                "SDI_DIRECT",
                "idem-key-1",
                Optional.of("EXT"),
                TransmissionStatus.PENDING_SUBMISSION,
                Optional.empty(),
                Instant.now(),
                Instant.now(),
                0,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                0,
                Map.of());

        repository.save(record);

        assertThrows(IllegalStateException.class, () -> repository.save(record));
    }

    @Test
    void outbox_retryableFailureRemainsProcessable() {
        InMemoryOutboxEventRepository repository = new InMemoryOutboxEventRepository();
        OutboxEvent event = new OutboxEvent(
                "evt-1",
                "TransmissionRecord",
                "tr-1",
                ServiceId.random(),
                "SubmissionRequested",
                "payload",
                OutboxEventStatus.PENDING,
                Instant.now(),
                Optional.empty(),
                Optional.empty(),
                0,
                Optional.empty(),
                Optional.empty()
        );
        repository.save(event);

        repository.tryMarkProcessing("evt-1", Instant.now());
        repository.markRetryableFailure("evt-1", "temporary", Instant.now().minusSeconds(1));

        assertEquals(1, repository.findProcessable(Instant.now(), 10).size());
    }

    private record TestFiscalDocument(String id, ServiceId serviceId, long version) implements FiscalDocument {
        @Override public CountryCode countryCode() { return CountryCode.of("IT"); }
        @Override public String documentType() { return "INVOICE"; }
        @Override public Party seller() { return party(); }
        @Override public Party buyer() { return party(); }
        @Override public List<DocumentLine> lines() { return List.of(); }
        @Override public Instant issuedAt() { return Instant.now(); }
        private Party party() { return new Party() {
            @Override public String id() { return "p"; }
            @Override public String role() { return "R"; }
            @Override public String legalName() { return "N"; }
            @Override public String vatNumber() { return "IT123"; }
        }; }
    }
}
