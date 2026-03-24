package com.acme.einvoice.grpc;

import com.acme.einvoice.application.exception.DocumentNotFoundException;
import com.acme.einvoice.application.exception.ValidationFailedException;
import com.acme.einvoice.application.usecase.SubmitDocumentCommand;
import com.acme.einvoice.application.usecase.SubmitDocumentResult;
import com.acme.einvoice.application.usecase.SubmitDocumentUseCase;
import com.acme.einvoice.common.model.ServiceId;

import java.util.UUID;

public class FiscalDocumentGrpcFacade {

    private final SubmitDocumentUseCase submitDocumentUseCase;

    public FiscalDocumentGrpcFacade(SubmitDocumentUseCase submitDocumentUseCase) {
        this.submitDocumentUseCase = submitDocumentUseCase;
    }

    public SubmitDocumentResult submitDocument(String serviceId, String documentId) {
        SubmitDocumentCommand command = new SubmitDocumentCommand(
                new ServiceId(UUID.fromString(serviceId)),
                documentId
        );
        try {
            return submitDocumentUseCase.execute(command);
        } catch (DocumentNotFoundException | ValidationFailedException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }
}
