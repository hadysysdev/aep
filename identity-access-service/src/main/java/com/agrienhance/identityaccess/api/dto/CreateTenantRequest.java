package com.agrienhance.identityaccess.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTenantRequest {
    @NotBlank(message = "Tenant name cannot be blank.")
    private String name;
    private String realmId; // e.g., "cooperative-xyz"
}