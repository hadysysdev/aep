package com.agrienhance.identityaccess.infrastructure.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class KeycloakAdminClientConfig {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.master-realm}")
    private String masterRealm;

    @Value("${keycloak.admin-client-id}")
    private String adminClientId;

    @Value("${keycloak.admin-client-secret}")
    private String adminClientSecret;

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(masterRealm)
                .grantType("client_credentials")
                .clientId(adminClientId)
                .clientSecret(adminClientSecret)
                .build();
    }
}