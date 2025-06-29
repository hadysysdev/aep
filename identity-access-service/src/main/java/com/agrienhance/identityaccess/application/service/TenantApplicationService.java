package com.agrienhance.identityaccess.application.service;

import com.agrienhance.identityaccess.api.dto.CreateTenantRequest;
import com.agrienhance.identityaccess.api.dto.TenantResponse;

public interface TenantApplicationService {
    TenantResponse createTenant(CreateTenantRequest request);
}