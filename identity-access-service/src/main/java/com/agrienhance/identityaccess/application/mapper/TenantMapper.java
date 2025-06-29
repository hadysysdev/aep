package com.agrienhance.identityaccess.application.mapper;

import com.agrienhance.identityaccess.api.dto.CreateTenantRequest;
import com.agrienhance.identityaccess.api.dto.TenantResponse;
import com.agrienhance.identityaccess.domain.entity.Tenant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TenantMapper {
    Tenant createRequestToTenant(CreateTenantRequest request);
    TenantResponse tenantToTenantResponse(Tenant tenant);
}