package com.acme.einvoice.application.service;

import com.acme.einvoice.domain.model.NormalizedTransmissionStatusUpdate;
import com.acme.einvoice.domain.model.TransmissionExternalEvent;
import com.acme.einvoice.domain.model.TransmissionRecord;

public interface ExternalStatusTranslator {
    NormalizedTransmissionStatusUpdate translate(TransmissionRecord transmission, TransmissionExternalEvent externalEvent);
}
