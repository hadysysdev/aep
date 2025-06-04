package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.config.AbstractIntegrationTest; // Inherit common config
import com.agrienhance.farmplot.domain.entity.Farm;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException; // For testing constraints

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

// No need for @Testcontainers or @Container here if AbstractIntegrationTest manages a singleton container
// and the DataSourceInitializer correctly points to it.
public class FarmRepositoryIT extends AbstractIntegrationTest { // Extends the base class

    @Autowired
    private FarmRepository farmRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326); // WGS84

    @AfterEach
    void tearDown() {
        // Clean up data after each test to ensure test independence
        farmRepository.deleteAll();
    }

    @Test
    void shouldSaveAndRetrieveFarm() {
        // Given
        UUID tenantId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Point location = geometryFactory.createPoint(new Coordinate(36.8219, -1.2921)); // Nairobi coordinates

        Farm farm = Farm.builder()
                .farmName("Test Kenya Farm")
                .ownerReferenceId(ownerId)
                .countryCode("KE")
                .region("Nairobi Area")
                .generalLocationCoordinates(location)
                .tenantId(tenantId)
                .notes("Integration test farm")
                .build();
        // @PrePersist will set farmIdentifier, createdAt, updatedAt, version

        // When
        Farm savedFarm = farmRepository.save(farm);

        // Then
        assertThat(savedFarm).isNotNull();
        assertThat(savedFarm.getFarmIdentifier()).isNotNull(); // Should be generated
        assertThat(savedFarm.getFarmName()).isEqualTo("Test Kenya Farm");
        assertThat(savedFarm.getTenantId()).isEqualTo(tenantId);
        assertThat(savedFarm.getGeneralLocationCoordinates()).isNotNull();
        assertThat(savedFarm.getGeneralLocationCoordinates().getX()).isEqualTo(36.8219);
        assertThat(savedFarm.getGeneralLocationCoordinates().getY()).isEqualTo(-1.2921);
        assertThat(savedFarm.getCreatedAt()).isNotNull();
        assertThat(savedFarm.getUpdatedAt()).isNotNull();
        assertThat(savedFarm.getVersion()).isNotNull().isEqualTo(0L); // Initial version is often 0 with Spring Data JPA

        // Retrieve and verify
        Optional<Farm> foundFarmOpt = farmRepository.findById(savedFarm.getFarmIdentifier());
        assertThat(foundFarmOpt).isPresent();
        Farm foundFarm = foundFarmOpt.get();
        assertThat(foundFarm.getFarmName()).isEqualTo(savedFarm.getFarmName());
        assertThat(foundFarm.getGeneralLocationCoordinates().equalsExact(location, 0.00001)).isTrue();
    }

    @Test
    void findByFarmIdentifierAndTenantId_whenExists_shouldReturnFarm() {
        // Given
        UUID tenantId = UUID.randomUUID();
        Farm farm = createAndSaveTestFarm("Specific Farm", tenantId);

        // When
        Optional<Farm> foundFarmOpt = farmRepository.findByFarmIdentifierAndTenantId(farm.getFarmIdentifier(),
                tenantId);

        // Then
        assertThat(foundFarmOpt).isPresent();
        assertThat(foundFarmOpt.get().getFarmName()).isEqualTo("Specific Farm");
    }

    @Test
    void findByFarmIdentifierAndTenantId_whenTenantIdMismatch_shouldReturnEmpty() {
        // Given
        UUID correctTenantId = UUID.randomUUID();
        UUID incorrectTenantId = UUID.randomUUID();
        Farm farm = createAndSaveTestFarm("Tenant Test Farm", correctTenantId);

        // When
        Optional<Farm> foundFarmOpt = farmRepository.findByFarmIdentifierAndTenantId(farm.getFarmIdentifier(),
                incorrectTenantId);

        // Then
        assertThat(foundFarmOpt).isNotPresent();
    }

    @Test
    void shouldEnforceNotNullConstraints() {
        // Example: countryCode is @NotNull
        Farm farm = Farm.builder()
                .farmName("Constraint Test")
                .ownerReferenceId(UUID.randomUUID())
                // .countryCode("US") // Missing countryCode
                .tenantId(UUID.randomUUID())
                .build();

        assertThrows(ConstraintViolationException.class, () -> {
            farmRepository.saveAndFlush(farm); // saveAndFlush to trigger constraint validation immediately
        });
    }

    // Helper method to create and save a farm for tests
    private Farm createAndSaveTestFarm(String name, UUID tenantId) {
        Farm farm = Farm.builder()
                .farmName(name)
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("XX")
                .tenantId(tenantId)
                .build();
        return farmRepository.save(farm);
    }
}