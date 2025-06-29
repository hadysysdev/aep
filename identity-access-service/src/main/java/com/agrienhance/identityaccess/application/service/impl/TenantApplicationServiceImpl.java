package com.agrienhance.identityaccess.application.service.impl;

import com.agrienhance.identityaccess.api.dto.CreateTenantRequest;
import com.agrienhance.identityaccess.api.dto.TenantResponse;
import com.agrienhance.identityaccess.application.mapper.TenantMapper;
import com.agrienhance.identityaccess.application.service.TenantApplicationService;
import com.agrienhance.identityaccess.domain.entity.Tenant;
import com.agrienhance.identityaccess.domain.repository.TenantRepository;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantApplicationServiceImpl implements TenantApplicationService {

    private final Keycloak keycloakAdminClient;
    private final TenantRepository tenantRepository;
    private final TenantMapper tenantMapper;

    @Override
    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        // 1. Prepare Realm Representation
        RealmRepresentation realmRepresentation = new RealmRepresentation();

        // Use provided realmId or generate one from the name for convenience
        String realmId = StringUtils.hasText(request.getRealmId())
                ? request.getRealmId()
                : generateRealmId(request.getName());

        realmRepresentation.setRealm(realmId);
        realmRepresentation.setDisplayName(request.getName());
        realmRepresentation.setEnabled(true);
        realmRepresentation.setRegistrationAllowed(false); // Only admins should create users
        realmRepresentation.setSslRequired("external"); // Recommended for production

        // 2. Create Realm in Keycloak
        try {
            keycloakAdminClient.realms().create(realmRepresentation);
            log.info("Successfully created Keycloak realm: {}", realmId);
        } catch (ClientErrorException e) {
            if (e.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new IllegalStateException("Realm with id '" + realmId + "' already exists.");
            }
            log.error("Client error while creating Keycloak realm '{}'", realmId, e);
            throw new RuntimeException("Failed to create Keycloak realm due to a client error.", e);
        }

        // 3. Save Tenant metadata to local DB
        Tenant tenant = Tenant.builder().name(request.getName()).keycloakRealmId(realmId).status("ACTIVE").build();

        Tenant savedTenant = tenantRepository.save(tenant);
        log.info("Successfully saved tenant metadata for tenantId: {}", savedTenant.getTenantId());

        // 4. Map to response DTO
        return tenantMapper.tenantToTenantResponse(savedTenant);
    }

    private String generateRealmId(String name) {
        // Create a URL-friendly slug from the tenant name
        return name.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9-]", "").trim();
    }
}