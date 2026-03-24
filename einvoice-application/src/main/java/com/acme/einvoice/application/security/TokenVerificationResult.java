package com.acme.einvoice.application.security;

import com.acme.einvoice.common.model.ServiceId;
import com.acme.einvoice.common.model.TenantId;

public record TokenVerificationResult(boolean valid, TenantId tenantId, ServiceId serviceId, String subject) {
}
