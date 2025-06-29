package com.agrienhance.identityaccess.api.controller;

import com.agrienhance.identityaccess.api.dto.CreateTenantRequest;
import com.agrienhance.identityaccess.config.AbstractIntegrationTest;
import com.agrienhance.identityaccess.domain.repository.TenantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.KeycloakTestConfig.class)
public class TenantControllerIT extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private TenantRepository tenantRepository;

        @Autowired
        private Keycloak keycloakAdminClient;

        @BeforeEach
        @AfterEach
        void cleanup() {
                tenantRepository.deleteAll();
                // Clean up any created realms
                keycloakAdminClient.realms().findAll().stream()
                                .filter(realm -> !realm.getRealm().equals("master"))
                                .forEach(realm -> keycloakAdminClient.realm(realm.getRealm()).remove());
        }

        @Test
        void createTenant_withValidRequest_shouldReturn201Created() throws Exception {
                // Given
                CreateTenantRequest request = CreateTenantRequest.builder()
                                .name("Test Cooperative")
                                .realmId("test-coop")
                                .build();

                // When & Then
                mockMvc.perform(post("/v1/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.tenantId", notNullValue()))
                                .andExpect(jsonPath("$.name", is("Test Cooperative")))
                                .andExpect(jsonPath("$.status", is("ACTIVE")));

                // Verify database record
                assertThat(tenantRepository.count()).isEqualTo(1);
                var tenant = tenantRepository.findAll().get(0);
                assertThat(tenant.getName()).isEqualTo("Test Cooperative");
                assertThat(tenant.getKeycloakRealmId()).isEqualTo("test-coop");

                // Verify Keycloak realm creation
                RealmRepresentation createdRealm = keycloakAdminClient.realm("test-coop").toRepresentation();
                assertThat(createdRealm).isNotNull();
                assertThat(createdRealm.getDisplayName()).isEqualTo("Test Cooperative");
                assertThat(createdRealm.isEnabled()).isTrue();
        }

        @Test
        void createTenant_withDuplicateRealmId_shouldReturn409Conflict() throws Exception {
                // Given: A tenant and realm already exist
                CreateTenantRequest request = CreateTenantRequest.builder().name("First Coop")
                                .realmId("duplicate-realm")
                                .build();
                mockMvc.perform(post("/v1/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)));

                // When: We try to create another tenant with the same realmId
                CreateTenantRequest duplicateRequest = CreateTenantRequest.builder().name("Second Coop")
                                .realmId("duplicate-realm").build();

                // Then
                mockMvc.perform(post("/v1/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(duplicateRequest)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message",
                                                is("Realm with id 'duplicate-realm' already exists.")));
        }

        @Test
        void createTenant_withInvalidRequest_missingName_shouldReturn400BadRequest() throws Exception {
                // Given
                CreateTenantRequest request = CreateTenantRequest.builder().name("").build(); // Invalid name

                // When & Then
                mockMvc.perform(post("/v1/tenants")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }
}