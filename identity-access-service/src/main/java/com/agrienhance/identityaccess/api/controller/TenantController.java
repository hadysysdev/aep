package com.agrienhance.identityaccess.api.controller;

import com.agrienhance.identityaccess.api.dto.CreateTenantRequest;
import com.agrienhance.identityaccess.api.dto.TenantResponse;
import com.agrienhance.identityaccess.application.service.TenantApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantApplicationService tenantApplicationService;

    @PostMapping
    @Operation(summary = "Onboard a new tenant by creating a Keycloak realm")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return new ResponseEntity<>(tenantApplicationService.createTenant(request), HttpStatus.CREATED);
    }
}