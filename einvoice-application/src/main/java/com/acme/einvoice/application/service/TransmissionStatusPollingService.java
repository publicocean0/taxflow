package com.acme.einvoice.application.service;

import com.acme.einvoice.application.exception.DocumentNotFoundException;
import com.acme.einvoice.application.routing.RoutingService;
import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.connectors.spi.ExternalStatusResult;
import com.acme.einvoice.connectors.spi.StatusQueryCommand;
import com.acme.einvoice.domain.model.TransmissionExternalEvent;
import com.acme.einvoice.domain.model.TransmissionRecord;
import com.acme.einvoice.domain.repository.FiscalDocumentRepository;
import com.acme.einvoice.domain.repository.TransmissionExternalEventRepository;
import com.acme.einvoice.domain.repository.TransmissionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TransmissionStatusPollingService {
    private final TransmissionRepository transmissionRepository;
    private final FiscalDocumentRepository fiscalDocumentRepository;
    private final TransmissionExternalEventRepository externalEventRepository;
    private final RoutingService routingService;

    public TransmissionStatusPollingService(
            TransmissionRepository transmissionRepository,
            FiscalDocumentRepository fiscalDocumentRepository,
            TransmissionExternalEventRepository externalEventRepository,
            RoutingService routingService
    ) {
        this.transmissionRepository = transmissionRepository;
        this.fiscalDocumentRepository = fiscalDocumentRepository;
        this.externalEventRepository = externalEventRepository;
        this.routingService = routingService;
    }

    public int pollEligibleTransmissions(ServiceId serviceId, int maxBatchSize) {
        List<TransmissionRecord> candidates = transmissionRepository.findDueForStatusCheck(serviceId, Instant.now(), maxBatchSize);
        int persisted = 0;
        for (TransmissionRecord transmission : candidates) {
            var document = fiscalDocumentRepository.findById(serviceId, transmission.documentId())
                    .orElseThrow(() -> new DocumentNotFoundException(transmission.documentId()));
            var connector = routingService.resolveConnector(serviceId, document.countryCode());
            Optional<ExternalStatusResult> externalStatus = connector.fetchStatus(new StatusQueryCommand(
                    serviceId,
                    document.countryCode(),
                    transmission.id(),
                    transmission.externalReference()
            ));
            if (externalStatus.isEmpty()) {
                continue;
            }
            ExternalStatusResult result = externalStatus.get();
            if (externalEventRepository.findByDeduplicationKey(serviceId, result.deduplicationKey()).isPresent()) {
                continue;
            }
            externalEventRepository.save(new TransmissionExternalEvent(
                    UUID.randomUUID().toString(),
                    transmission.serviceId(),
                    Optional.ofNullable(transmission.details().get("tenantId")),
                    transmission.id(),
                    Optional.of(transmission.documentId()),
                    result.sourceType(),
                    result.externalStatusCode(),
                    result.externalStatusLabel(),
                    result.externalReference(),
                    result.deduplicationKey(),
                    result.rawPayload(),
                    result.occurredAtExternal(),
                    result.receivedAt(),
                    Optional.empty(),
                    Optional.empty(),
                    false,
                    Instant.now(),
                    result.metadata()
            ));
            persisted++;
        }
        return persisted;
    }
}
