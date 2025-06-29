package com.agrienhance.identityaccess.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TenantResponse {
    private UUID tenantId;
    private String name;
    private String status;
    private Instant createdAt;
}