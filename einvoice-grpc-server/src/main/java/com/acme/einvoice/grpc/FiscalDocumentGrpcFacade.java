package com.acme.einvoice.grpc;

import com.acme.einvoice.application.usecase.SubmitDocumentCommand;
import com.acme.einvoice.application.usecase.SubmitDocumentResult;
import com.acme.einvoice.application.usecase.SubmitDocumentUseCase;
import com.acme.einvoice.common.model.TenantId;

import java.util.UUID;

public class FiscalDocumentGrpcFacade {

    private final SubmitDocumentUseCase submitDocumentUseCase;

    public FiscalDocumentGrpcFacade(SubmitDocumentUseCase submitDocumentUseCase) {
        this.submitDocumentUseCase = submitDocumentUseCase;
    }

    public SubmitDocumentResult submitDocument(String tenantId, String documentId) {
        SubmitDocumentCommand command = new SubmitDocumentCommand(new TenantId(UUID.fromString(tenantId)), documentId);
        return submitDocumentUseCase.execute(command);
    }
}
