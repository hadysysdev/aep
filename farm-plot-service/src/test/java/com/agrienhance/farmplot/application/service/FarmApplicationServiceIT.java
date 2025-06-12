package com.agrienhance.farmplot.application.service;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.FarmResponse;
import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.config.AbstractIntegrationTest; // Our Testcontainers base class
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional; // Important for test methods

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
// Import ResourceNotFoundException if you test for it
import com.agrienhance.farmplot.application.exception.ResourceNotFoundException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.agrienhance.farmplot.api.dto.farm.UpdateFarmRequest; // Add this
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Transactional // Ensures each test method runs in a transaction and rolls back
public class FarmApplicationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private FarmApplicationService farmApplicationService; // The service we are testing

    @Autowired
    private FarmRepository farmRepository; // To directly verify database state

    @PersistenceContext
    private EntityManager entityManager;

    private UUID tenantId;
    private UUID ownerId;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        farmRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();

        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
    }

    @Test
    void createFarm_shouldPersistFarmAndReturnFarmResponse() {
        // Given
        PointGeometryDto locationDto = PointGeometryDto.builder()
                .type("Point")
                .coordinates(List.of(34.0, -1.0))
                .build();

        CreateFarmRequest request = CreateFarmRequest.builder()
                .farmName("Service Test Farm")
                .ownerReferenceId(ownerId)
                .countryCode("SV")
                .region("Service Region")
                .generalLocationCoordinates(locationDto)
                .notes("Testing farm creation via service")
                // .tenantId(tenantId)
                .build();

        // When
        FarmResponse createdFarmResponse = farmApplicationService.createFarm(request, tenantId);

        // Then
        assertThat(createdFarmResponse).isNotNull();
        assertThat(createdFarmResponse.getFarmIdentifier()).isNotNull();
        assertThat(createdFarmResponse.getFarmName()).isEqualTo("Service Test Farm");
        assertThat(createdFarmResponse.getOwnerReferenceId()).isEqualTo(ownerId);
        assertThat(createdFarmResponse.getTenantId()).isEqualTo(tenantId);
        assertThat(createdFarmResponse.getGeneralLocationCoordinates()).isNotNull();
        assertThat(createdFarmResponse.getGeneralLocationCoordinates().getCoordinates().get(0)).isEqualTo(34.0);

        // Verify directly from database
        entityManager.flush(); // Ensure data is flushed to DB if not already by service's @Transactional
        entityManager.clear(); // Detach to ensure fresh read

        Optional<Farm> foundFarmOpt = farmRepository.findById(createdFarmResponse.getFarmIdentifier());
        assertThat(foundFarmOpt).isPresent();
        Farm persistedFarm = foundFarmOpt.get();
        assertThat(persistedFarm.getFarmName()).isEqualTo("Service Test Farm");
        assertThat(persistedFarm.getTenantId()).isEqualTo(tenantId);
        assertThat(persistedFarm.getGeneralLocationCoordinates()).isNotNull();
        assertThat(persistedFarm.getGeneralLocationCoordinates().getX()).isEqualTo(34.0);
    }

    @Test
    void getFarmById_whenFarmExistsAndTenantMatches_shouldReturnFarmResponse() {
        // Given: First create a farm
        PointGeometryDto locationDto = PointGeometryDto.builder().type("Point").coordinates(List.of(35.0, -2.0))
                .build();
        CreateFarmRequest createRequest = CreateFarmRequest.builder()
                .farmName("Fetch Test Farm")
                .ownerReferenceId(ownerId)
                .countryCode("FT")
                // .tenantId(tenantId)
                .generalLocationCoordinates(locationDto)
                .build();
        FarmResponse createdFarm = farmApplicationService.createFarm(createRequest, tenantId);
        entityManager.flush();
        entityManager.clear();

        // When
        FarmResponse foundFarmResponse = farmApplicationService.getFarmById(createdFarm.getFarmIdentifier(), tenantId);

        // Then
        assertThat(foundFarmResponse).isNotNull();
        assertThat(foundFarmResponse.getFarmIdentifier()).isEqualTo(createdFarm.getFarmIdentifier());
        assertThat(foundFarmResponse.getFarmName()).isEqualTo("Fetch Test Farm");
    }

    @Test
    void getFarmById_whenFarmExistsButTenantMismatch_shouldThrowResourceNotFound() {
        // Given: Create a farm with 'tenantId'
        PointGeometryDto locationDto = PointGeometryDto.builder().type("Point").coordinates(List.of(35.0, -2.0))
                .build();
        CreateFarmRequest createRequest = CreateFarmRequest.builder()
                .farmName("Tenant Mismatch Farm")
                .ownerReferenceId(ownerId)
                .countryCode("TM")
                // .tenantId(tenantId) // Original tenant
                .generalLocationCoordinates(locationDto)
                .build();
        FarmResponse createdFarm = farmApplicationService.createFarm(createRequest, tenantId);
        entityManager.flush();
        entityManager.clear();

        UUID otherTenantId = UUID.randomUUID(); // Different tenant

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            farmApplicationService.getFarmById(createdFarm.getFarmIdentifier(), otherTenantId);
        });
    }

    @Test
    void getFarmById_whenFarmDoesNotExist_shouldThrowResourceNotFound() {
        // Given
        UUID nonExistentFarmId = UUID.randomUUID();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            farmApplicationService.getFarmById(nonExistentFarmId, tenantId);
        });
    }

    // TODO: Add tests for updateFarm, deleteFarm, listFarmsByTenant
    // For updateFarm, ensure only allowed fields are updated and tenant isolation
    // is maintained.
    // For deleteFarm, ensure the farm is deleted and trying to fetch it again
    // results in ResourceNotFound.
    // For listFarmsByTenant, verify pagination and that only farms for the given
    // tenant are returned.

    @Test
    void updateFarm_whenFarmExistsAndTenantMatches_shouldUpdateAndReturnFarmResponse() {
        // Given: First create a farm
        CreateFarmRequest createRequest = CreateFarmRequest.builder()
                .farmName("Original Farm Name")
                .ownerReferenceId(ownerId)
                .countryCode("OG") // Original Country
                .region("Original Region")
                .generalLocationCoordinates(
                        PointGeometryDto.builder().type("Point").coordinates(List.of(1.0, 1.0)).build())
                .notes("Original notes")
                // .tenantId(tenantId)
                .build();
        FarmResponse createdFarm = farmApplicationService.createFarm(createRequest, tenantId);
        entityManager.flush();
        entityManager.clear();

        UpdateFarmRequest updateRequest = UpdateFarmRequest.builder()
                .farmName("Updated Farm Name")
                .region("Updated Region")
                .notes(null) // Test setting a field to null explicitly if allowed by DTO/mapper
                .generalLocationCoordinates(
                        PointGeometryDto.builder().type("Point").coordinates(List.of(2.0, 2.0)).build())
                // countryCode is not in UpdateFarmRequest, so it should remain "OG"
                // ownerReferenceId and tenantId are not updatable via this DTO based on our
                // earlier design
                .build();

        // When
        FarmResponse updatedFarmResponse = farmApplicationService.updateFarm(createdFarm.getFarmIdentifier(),
                updateRequest, tenantId);

        // Then
        assertThat(updatedFarmResponse).isNotNull();
        assertThat(updatedFarmResponse.getFarmIdentifier()).isEqualTo(createdFarm.getFarmIdentifier());
        assertThat(updatedFarmResponse.getFarmName()).isEqualTo("Updated Farm Name");
        assertThat(updatedFarmResponse.getRegion()).isEqualTo("Updated Region");
        assertThat(updatedFarmResponse.getCountryCode()).isEqualTo("OG"); // Should not have changed
        assertThat(updatedFarmResponse.getNotes()).isNull(); // Was set to null
        assertThat(updatedFarmResponse.getGeneralLocationCoordinates().getCoordinates()).containsExactly(2.0, 2.0);
        assertThat(updatedFarmResponse.getOwnerReferenceId()).isEqualTo(ownerId); // Should not change
        assertThat(updatedFarmResponse.getTenantId()).isEqualTo(tenantId); // Should not change

        // Verify directly from database
        entityManager.flush();
        entityManager.clear();
        Farm dbFarm = farmRepository.findById(createdFarm.getFarmIdentifier()).orElseThrow();
        assertThat(dbFarm.getFarmName()).isEqualTo("Updated Farm Name");
        assertThat(dbFarm.getRegion()).isEqualTo("Updated Region");
        assertThat(dbFarm.getNotes()).isNull();
        assertThat(dbFarm.getGeneralLocationCoordinates().getX()).isEqualTo(2.0);
    }

    @Test
    void updateFarm_whenFarmNotFound_shouldThrowResourceNotFoundException() {
        // Given
        UUID nonExistentFarmId = UUID.randomUUID();
        UpdateFarmRequest updateRequest = UpdateFarmRequest.builder().farmName("Doesn't matter").build();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            farmApplicationService.updateFarm(nonExistentFarmId, updateRequest, tenantId);
        });
    }

    @Test
    void updateFarm_whenTenantMismatch_shouldThrowResourceNotFoundException() {
        // Given: Create a farm
        CreateFarmRequest createRequest = CreateFarmRequest.builder()
                .farmName("Tenant Specific Farm")
                .ownerReferenceId(ownerId)
                .countryCode("TS")
                // .tenantId(tenantId)
                .build();
        FarmResponse createdFarm = farmApplicationService.createFarm(createRequest, tenantId);
        entityManager.flush();
        entityManager.clear();

        UUID otherTenantId = UUID.randomUUID();
        UpdateFarmRequest updateRequest = UpdateFarmRequest.builder().farmName("Attempted Update").build();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            farmApplicationService.updateFarm(createdFarm.getFarmIdentifier(), updateRequest, otherTenantId);
        });
    }

    @Test
    void deleteFarm_whenFarmExistsAndTenantMatches_shouldDeleteFarm() {
        // Given: Create a farm
        CreateFarmRequest createRequest = CreateFarmRequest.builder()
                .farmName("Farm to Delete")
                .ownerReferenceId(ownerId)
                .countryCode("TD")
                // .tenantId(tenantId)
                .build();
        FarmResponse createdFarm = farmApplicationService.createFarm(createRequest, tenantId);
        assertThat(farmRepository.existsById(createdFarm.getFarmIdentifier())).isTrue();
        entityManager.flush(); // Ensure create is committed before delete if delete starts new tx
        entityManager.clear();

        // When
        farmApplicationService.deleteFarm(createdFarm.getFarmIdentifier(), tenantId);
        entityManager.flush(); // Ensure delete is committed
        entityManager.clear();

        // Then
        Optional<Farm> deletedFarmOpt = farmRepository.findById(createdFarm.getFarmIdentifier());
        assertThat(deletedFarmOpt).isNotPresent();
        assertThat(farmRepository.existsById(createdFarm.getFarmIdentifier())).isFalse();
    }

    @Test
    void deleteFarm_whenFarmNotFound_shouldThrowResourceNotFoundException() {
        // Given
        UUID nonExistentFarmId = UUID.randomUUID();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            farmApplicationService.deleteFarm(nonExistentFarmId, tenantId);
        });
    }

    @Test
    void deleteFarm_whenTenantMismatch_shouldThrowResourceNotFoundException() {
        // Given: Create a farm
        CreateFarmRequest createRequest = CreateFarmRequest.builder()
                .farmName("Delete Tenant Mismatch Farm")
                .ownerReferenceId(ownerId)
                .countryCode("DT")
                // .tenantId(tenantId)
                .build();
        FarmResponse createdFarm = farmApplicationService.createFarm(createRequest, tenantId);
        entityManager.flush();
        entityManager.clear();

        UUID otherTenantId = UUID.randomUUID();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            farmApplicationService.deleteFarm(createdFarm.getFarmIdentifier(), otherTenantId);
        });
        // Also assert that the farm still exists for the original tenant
        assertThat(farmRepository.findByFarmIdentifierAndTenantId(createdFarm.getFarmIdentifier(), tenantId))
                .isPresent();
    }

    @Test
    void listFarmsByTenant_shouldReturnOnlyFarmsForGivenTenantAndPage() {
        // Given
        UUID otherTenantId = UUID.randomUUID();
        // Farms for target tenantId
        farmApplicationService.createFarm(CreateFarmRequest.builder().farmName("Farm A (Tenant 1)")
                .ownerReferenceId(ownerId).countryCode("T1").build(), tenantId);
        farmApplicationService.createFarm(CreateFarmRequest.builder().farmName("Farm B (Tenant 1)")
                .ownerReferenceId(ownerId).countryCode("T1").build(), tenantId);
        farmApplicationService.createFarm(CreateFarmRequest.builder().farmName("Farm C (Tenant 1)")
                .ownerReferenceId(ownerId).countryCode("T1").build(), tenantId);
        // Farm for another tenant
        farmApplicationService.createFarm(CreateFarmRequest.builder().farmName("Farm D (Tenant 2)")
                .ownerReferenceId(ownerId).countryCode("T2").build(), otherTenantId);
        entityManager.flush();
        entityManager.clear();

        // When: Get first page, size 2
        Pageable pageable = PageRequest.of(0, 2, Sort.by("farmName").ascending());
        Page<FarmResponse> farmPage = farmApplicationService.listFarmsByTenant(tenantId, pageable);

        // Then
        assertThat(farmPage).isNotNull();
        assertThat(farmPage.getTotalElements()).isEqualTo(3); // Total 3 farms for tenantId
        assertThat(farmPage.getTotalPages()).isEqualTo(2); // 3 items, page size 2 -> 2 pages
        assertThat(farmPage.getNumberOfElements()).isEqualTo(2); // Current page has 2 items
        assertThat(farmPage.getContent()).hasSize(2)
                .extracting(FarmResponse::getFarmName)
                .containsExactly("Farm A (Tenant 1)", "Farm B (Tenant 1)"); // Assuming alphabetical sort

        // When: Get second page
        pageable = PageRequest.of(1, 2, Sort.by("farmName").ascending());
        farmPage = farmApplicationService.listFarmsByTenant(tenantId, pageable);

        // Then
        assertThat(farmPage.getNumberOfElements()).isEqualTo(1); // Current page has 1 item
        assertThat(farmPage.getContent()).hasSize(1)
                .extracting(FarmResponse::getFarmName)
                .containsExactly("Farm C (Tenant 1)");
    }

    @Test
    void listFarmsByTenant_whenNoFarmsForTenant_shouldReturnEmptyPage() {
        // Given
        UUID nonExistentTenantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<FarmResponse> farmPage = farmApplicationService.listFarmsByTenant(nonExistentTenantId, pageable);

        // Then
        assertThat(farmPage).isNotNull();
        assertThat(farmPage.getTotalElements()).isEqualTo(0);
        assertThat(farmPage.getContent()).isEmpty();
    }

}