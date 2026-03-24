package com.acme.einvoice.application.usecase;

public interface SubmitDocumentUseCase {
    SubmitDocumentResult execute(SubmitDocumentCommand command);
}
