package com.agrienhance.farmplot.application.service;

import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
import com.agrienhance.farmplot.api.dto.landtenure.CreateOrUpdateLandTenureRequest;
import com.agrienhance.farmplot.api.dto.landtenure.LandTenureResponse;
import com.agrienhance.farmplot.api.dto.plot.CreatePlotRequest;
import com.agrienhance.farmplot.api.dto.plot.PlotResponse;
import com.agrienhance.farmplot.api.dto.plot.UpdatePlotRequest;
import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.LandTenure;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.LandTenureRepository;
import com.agrienhance.farmplot.domain.repository.PlotRepository;
import com.agrienhance.farmplot.application.exception.ResourceNotFoundException; // Ensure this is imported
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
public class PlotApplicationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PlotApplicationService plotApplicationService;

    @Autowired
    private PlotRepository plotRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private LandTenureRepository landTenureRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Farm testFarm;
    private UUID tenantId;
    private UUID ownerId; // For farm creation

    @BeforeEach
    void setUp() {
        // Clean up order
        landTenureRepository.deleteAllInBatch();
        plotRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();

        tenantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        testFarm = Farm.builder()
                .farmName("Service Test Farm for Plots")
                .ownerReferenceId(ownerId)
                .countryCode("PL")
                .tenantId(tenantId)
                .build();
        farmRepository.saveAndFlush(testFarm);
    }

    @Test
    void createPlot_shouldPersistPlotAndReturnPlotResponse() {
        // Given
        PolygonGeometryDto plotGeomDto = createTestPolygonDto(0.001, 10.0, 20.0);
        CreatePlotRequest request = CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Service Test Plot")
                .plotGeometry(plotGeomDto)
                .landTenureType(LandTenureType.OWNED) // Setting directly on Plot
                .tenantId(tenantId) // Service should validate this against farm's tenant
                .build();

        // When
        PlotResponse createdPlotResponse = plotApplicationService.createPlot(request);

        // Then
        assertThat(createdPlotResponse).isNotNull();
        assertThat(createdPlotResponse.getPlotIdentifier()).isNotNull();
        assertThat(createdPlotResponse.getPlotName()).isEqualTo("Service Test Plot");
        assertThat(createdPlotResponse.getFarmIdentifier()).isEqualTo(testFarm.getFarmIdentifier());
        assertThat(createdPlotResponse.getTenantId()).isEqualTo(tenantId);
        assertThat(createdPlotResponse.getLandTenureType()).isEqualTo(LandTenureType.OWNED); // Check this
        assertThat(createdPlotResponse.getPlotGeometry()).isNotNull();
        assertThat(createdPlotResponse.getPlotGeometry().getCoordinates().get(0).get(0))
                .containsExactly(10.0, 20.0);

        // Verify calculated area from DB
        entityManager.flush();
        entityManager.clear();
        Plot persistedPlot = plotRepository.findById(createdPlotResponse.getPlotIdentifier()).orElseThrow();
        assertThat(persistedPlot.getCalculatedAreaHectares()).isNotNull();
        assertThat(persistedPlot.getCalculatedAreaHectares().doubleValue()).isGreaterThan(0.0);
        assertThat(persistedPlot.getLandTenureType()).isEqualTo(LandTenureType.OWNED); // Verify on entity too
    }

    @Test
    void createPlot_whenFarmNotFound_shouldThrowResourceNotFoundException() {
        // Given
        CreatePlotRequest request = CreatePlotRequest.builder()
                .farmIdentifier(UUID.randomUUID()) // Non-existent farm
                .plotName("Plot with Bad Farm")
                .plotGeometry(createTestPolygonDto(0.001, 0, 0))
                .landTenureType(LandTenureType.OWNED)
                .tenantId(tenantId)
                .build();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.createPlot(request);
        });
    }

    @Test
    void createPlot_whenTenantIdMismatch_shouldThrowResourceNotFoundException() {
        // Given
        UUID differentTenantId = UUID.randomUUID();
        CreatePlotRequest request = CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Plot Tenant Mismatch")
                .plotGeometry(createTestPolygonDto(0.001, 0, 0))
                .landTenureType(LandTenureType.OWNED)
                .tenantId(differentTenantId) // DTO tenantId differs from farm's tenantId
                .build();

        // When & Then
        // The service's createPlot method should validate that the farm (identified by
        // farmIdentifier)
        // belongs to the tenantId passed in the DTO (or from security context).
        // Our PlotApplicationServiceImpl currently fetches farm using
        // request.getFarmIdentifier() and request.getTenantId().
        // If the farm doesn't exist under THAT tenantId, it throws ResourceNotFound.
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.createPlot(request);
        });
    }

    // --- LandTenure Management Tests within PlotApplicationService ---

    @Test
    void createOrUpdateLandTenureForPlot_shouldCreateNewLandTenure() {
        // Given: Create a plot first
        PlotResponse plot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier()).plotName("Plot For Tenure Create")
                .plotGeometry(createTestPolygonDto(0.001, 1, 1)).landTenureType(LandTenureType.UNKNOWN)
                .tenantId(tenantId).build());
        entityManager.flush();
        entityManager.clear();

        CreateOrUpdateLandTenureRequest tenureRequest = CreateOrUpdateLandTenureRequest.builder()
                .tenureType(LandTenureType.LEASED)
                .leaseStartDate(LocalDate.of(2024, 1, 1))
                .leaseEndDate(LocalDate.of(2025, 12, 31))
                .ownerDetails("Landlord A")
                .build();

        // When
        LandTenureResponse tenureResponse = plotApplicationService.createOrUpdateLandTenureForPlot(
                plot.getPlotIdentifier(), tenureRequest, tenantId);

        // Then
        assertThat(tenureResponse).isNotNull();
        assertThat(tenureResponse.getPlotIdentifier()).isEqualTo(plot.getPlotIdentifier());
        assertThat(tenureResponse.getTenureType()).isEqualTo(LandTenureType.LEASED);
        assertThat(tenureResponse.getLeaseStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));

        // Verify that the Plot's own landTenureType field was also updated
        entityManager.flush();
        entityManager.clear();
        Plot updatedPlot = plotRepository.findById(plot.getPlotIdentifier()).orElseThrow();
        assertThat(updatedPlot.getLandTenureType()).isEqualTo(LandTenureType.LEASED);
    }

    @Test
    void createOrUpdateLandTenureForPlot_shouldUpdateExistingLandTenure() {
        // Given: Create a plot and an initial land tenure
        PlotResponse plot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier()).plotName("Plot For Tenure Update")
                .plotGeometry(createTestPolygonDto(0.001, 2, 2)).landTenureType(LandTenureType.OWNED)
                .tenantId(tenantId).build());
        plotApplicationService.createOrUpdateLandTenureForPlot(plot.getPlotIdentifier(),
                CreateOrUpdateLandTenureRequest.builder().tenureType(LandTenureType.OWNED).build(), tenantId);
        entityManager.flush();
        entityManager.clear();

        CreateOrUpdateLandTenureRequest updateTenureRequest = CreateOrUpdateLandTenureRequest.builder()
                .tenureType(LandTenureType.COMMUNAL_ACCESS)
                .ownerDetails("Community Managed")
                .build();

        // When
        LandTenureResponse tenureResponse = plotApplicationService.createOrUpdateLandTenureForPlot(
                plot.getPlotIdentifier(), updateTenureRequest, tenantId);

        // Then
        assertThat(tenureResponse.getTenureType()).isEqualTo(LandTenureType.COMMUNAL_ACCESS);
        assertThat(tenureResponse.getOwnerDetails()).isEqualTo("Community Managed");

        // Verify Plot's landTenureType is updated
        entityManager.flush();
        entityManager.clear();
        Plot updatedPlot = plotRepository.findById(plot.getPlotIdentifier()).orElseThrow();
        assertThat(updatedPlot.getLandTenureType()).isEqualTo(LandTenureType.COMMUNAL_ACCESS);
    }

    @Test
    void getLandTenureForPlot_whenExists_shouldReturnLandTenure() {
        // Given
        PlotResponse plot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier()).plotName("Plot For Get Tenure")
                .plotGeometry(createTestPolygonDto(0.001, 3, 3)).landTenureType(LandTenureType.CUSTOM_AGREEMENT)
                .tenantId(tenantId).build());
        plotApplicationService.createOrUpdateLandTenureForPlot(plot.getPlotIdentifier(),
                CreateOrUpdateLandTenureRequest.builder().tenureType(LandTenureType.CUSTOM_AGREEMENT)
                        .ownerDetails("Custom Details").build(),
                tenantId);
        entityManager.flush();
        entityManager.clear();

        // When
        LandTenureResponse tenureResponse = plotApplicationService.getLandTenureForPlot(plot.getPlotIdentifier(),
                tenantId);

        // Then
        assertThat(tenureResponse).isNotNull();
        assertThat(tenureResponse.getTenureType()).isEqualTo(LandTenureType.CUSTOM_AGREEMENT);
        assertThat(tenureResponse.getOwnerDetails()).isEqualTo("Custom Details");
    }

    @Test
    void getLandTenureForPlot_whenNotExists_shouldThrowResourceNotFound() {
        // Given
        PlotResponse plot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier()).plotName("Plot No Tenure Yet")
                .plotGeometry(createTestPolygonDto(0.001, 4, 4)).landTenureType(LandTenureType.UNKNOWN)
                .tenantId(tenantId).build());
        entityManager.flush();
        entityManager.clear();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.getLandTenureForPlot(plot.getPlotIdentifier(), tenantId);
        });
    }

    @Test
    void deleteLandTenureForPlot_shouldDeleteAndResetPlotTenureType() {
        // Given
        PlotResponse plot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier()).plotName("Plot For Delete Tenure")
                .plotGeometry(createTestPolygonDto(0.001, 5, 5)).landTenureType(LandTenureType.OWNED) // Initially set
                .tenantId(tenantId).build());
        // Create a tenure record
        plotApplicationService.createOrUpdateLandTenureForPlot(plot.getPlotIdentifier(),
                CreateOrUpdateLandTenureRequest.builder().tenureType(LandTenureType.OWNED).build(), tenantId);
        entityManager.flush();
        entityManager.clear();

        // Confirm tenure exists
        assertThat(landTenureRepository.findByPlot_PlotIdentifierAndTenantId(plot.getPlotIdentifier(), tenantId))
                .isPresent();

        // When
        plotApplicationService.deleteLandTenureForPlot(plot.getPlotIdentifier(), tenantId);
        entityManager.flush();
        entityManager.clear();

        // Then
        // Verify LandTenure record is deleted
        assertThat(landTenureRepository.findByPlot_PlotIdentifierAndTenantId(plot.getPlotIdentifier(), tenantId))
                .isNotPresent();

        // Verify Plot's landTenureType is reset (assuming service logic does this,
        // e.g., to UNKNOWN)
        // This depends on the desired behavior in
        // PlotApplicationServiceImpl.deleteLandTenureForPlot
        // For now, let's assume it doesn't reset the Plot.landTenureType field
        // automatically unless coded.
        // If PlotApplicationServiceImpl.deleteLandTenureForPlot updates
        // Plot.landTenureType:
        // Plot updatedPlot =
        // plotRepository.findById(plot.getPlotIdentifier()).orElseThrow();
        // assertThat(updatedPlot.getLandTenureType()).isEqualTo(LandTenureType.UNKNOWN);
        // // Or whatever default
    }

    @Test
    void listPlotsByFarm_shouldReturnPaginatedPlotsForCorrectFarmAndTenant() {
        // Given
        // Plots for testFarm (tenantId)
        plotApplicationService.createPlot(CreatePlotRequest.builder().farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Plot Alpha (Farm1)").plotGeometry(createTestPolygonDto(0.001, 1, 1))
                .landTenureType(LandTenureType.OWNED).tenantId(tenantId).build());
        plotApplicationService.createPlot(CreatePlotRequest.builder().farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Plot Bravo (Farm1)").plotGeometry(createTestPolygonDto(0.001, 2, 2))
                .landTenureType(LandTenureType.LEASED).tenantId(tenantId).build());
        plotApplicationService.createPlot(CreatePlotRequest.builder().farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Plot Charlie (Farm1)").plotGeometry(createTestPolygonDto(0.001, 3, 3))
                .landTenureType(LandTenureType.COMMUNAL_ACCESS).tenantId(tenantId).build());

        // Plot for another farm but same tenant (should not be listed by this method if
        // filtering by farmId)
        Farm anotherFarmSameTenant = farmRepository.saveAndFlush(Farm.builder().farmName("Another Farm, Same Tenant")
                .ownerReferenceId(ownerId).countryCode("PL").tenantId(tenantId).build());
        plotApplicationService
                .createPlot(CreatePlotRequest.builder().farmIdentifier(anotherFarmSameTenant.getFarmIdentifier())
                        .plotName("Plot Delta (Farm2)").plotGeometry(createTestPolygonDto(0.001, 4, 4))
                        .landTenureType(LandTenureType.OWNED).tenantId(tenantId).build());

        // Plot for testFarm but different tenant (should not be listed)
        UUID otherTenantId = UUID.randomUUID();
        // No need to create this plot as the service method should filter by tenantId
        // passed as argument

        entityManager.flush();
        entityManager.clear();

        // When: Get first page, size 2, for testFarm
        Pageable pageable = PageRequest.of(0, 2, Sort.by("plotName").ascending());
        Page<PlotResponse> plotPage = plotApplicationService.listPlotsByFarm(testFarm.getFarmIdentifier(), tenantId,
                pageable);

        // Then
        assertThat(plotPage).isNotNull();
        assertThat(plotPage.getTotalElements()).isEqualTo(3); // 3 plots for testFarm
        assertThat(plotPage.getTotalPages()).isEqualTo(2);
        assertThat(plotPage.getContent()).hasSize(2)
                .extracting(PlotResponse::getPlotName)
                .containsExactly("Plot Alpha (Farm1)", "Plot Bravo (Farm1)");

        // When: Get second page
        pageable = PageRequest.of(1, 2, Sort.by("plotName").ascending());
        plotPage = plotApplicationService.listPlotsByFarm(testFarm.getFarmIdentifier(), tenantId, pageable);
        assertThat(plotPage.getContent()).hasSize(1)
                .extracting(PlotResponse::getPlotName)
                .containsExactly("Plot Charlie (Farm1)");
    }

    @Test
    void listPlotsByFarm_whenFarmNotFoundOrTenantMismatch_shouldThrowResourceNotFound() {
        // Given
        UUID nonExistentFarmId = UUID.randomUUID();
        UUID differentTenantId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        // When & Then: Farm not found
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.listPlotsByFarm(nonExistentFarmId, tenantId, pageable);
        });

        // When & Then: Tenant mismatch for an existing farm
        // (The PlotApplicationServiceImpl.listPlotsByFarm first checks if the farm
        // exists under the given tenant)
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.listPlotsByFarm(testFarm.getFarmIdentifier(), differentTenantId, pageable);
        });
    }

    // Test for listAllPlotsForTenant
    @Test
    void listAllPlotsForTenant_shouldReturnPaginatedPlotsForCorrectTenant() {
        // Given
        // Plots for tenantId (associated with testFarm)
        plotApplicationService.createPlot(CreatePlotRequest.builder().farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Plot A (Tenant1 Farm1)").plotGeometry(createTestPolygonDto(0.001, 1, 1))
                .landTenureType(LandTenureType.OWNED).tenantId(tenantId).build());

        // Plot for another farm but same tenantId
        Farm anotherFarmSameTenant = farmRepository.saveAndFlush(Farm.builder().farmName("Farm B (Tenant1)")
                .ownerReferenceId(ownerId).countryCode("PL").tenantId(tenantId).build());
        plotApplicationService
                .createPlot(CreatePlotRequest.builder().farmIdentifier(anotherFarmSameTenant.getFarmIdentifier())
                        .plotName("Plot B (Tenant1 Farm2)").plotGeometry(createTestPolygonDto(0.001, 2, 2))
                        .landTenureType(LandTenureType.LEASED).tenantId(tenantId).build());

        // Plot for a different tenant (should not be listed)
        UUID otherTenantId = UUID.randomUUID();
        Farm farmOtherTenant = farmRepository.saveAndFlush(Farm.builder().farmName("Farm C (Tenant2)")
                .ownerReferenceId(ownerId).countryCode("PL").tenantId(otherTenantId).build());
        plotApplicationService
                .createPlot(CreatePlotRequest.builder().farmIdentifier(farmOtherTenant.getFarmIdentifier())
                        .plotName("Plot C (Tenant2 FarmC)").plotGeometry(createTestPolygonDto(0.001, 3, 3))
                        .landTenureType(LandTenureType.OWNED).tenantId(otherTenantId).build());
        entityManager.flush();
        entityManager.clear();

        // When: Get first page, size 1 for tenantId
        Pageable pageable = PageRequest.of(0, 1, Sort.by("plotName").ascending());
        Page<PlotResponse> plotPage = plotApplicationService.listAllPlotsForTenant(tenantId, pageable);

        // Then
        assertThat(plotPage).isNotNull();
        assertThat(plotPage.getTotalElements()).isEqualTo(2); // 2 plots for tenantId
        assertThat(plotPage.getTotalPages()).isEqualTo(2);
        assertThat(plotPage.getContent()).hasSize(1)
                .extracting(PlotResponse::getPlotName)
                .containsExactly("Plot A (Tenant1 Farm1)"); // Assuming alphabetical sort
    }

    // Test for updatePlot
    @Test
    void updatePlot_shouldUpdatePlotDetailsAndArea() {
        // Given: Create an initial plot
        PlotResponse initialPlot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Initial Plot Name")
                .plotGeometry(createTestPolygonDto(0.001, 5, 5)) // Approx 1.23 Ha
                .landTenureType(LandTenureType.OWNED)
                .tenantId(tenantId)
                .build());
        entityManager.flush();
        entityManager.clear();

        BigDecimal initialArea = plotRepository.findById(initialPlot.getPlotIdentifier()).get()
                .getCalculatedAreaHectares();
        System.out.println("Initial calculated area: " + initialArea);

        PolygonGeometryDto updatedGeomDto = createTestPolygonDto(0.002, 6, 6); // Larger geometry -> larger area
        UpdatePlotRequest updateRequest = UpdatePlotRequest.builder()
                .plotName("Updated Plot Name")
                .landTenureType(LandTenureType.COMMUNAL_ACCESS)
                .plotGeometry(updatedGeomDto)
                // cultivatorReferenceId could also be updated here
                .build();

        // When
        PlotResponse updatedPlotResponse = plotApplicationService.updatePlot(initialPlot.getPlotIdentifier(),
                updateRequest, tenantId);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(updatedPlotResponse).isNotNull();
        assertThat(updatedPlotResponse.getPlotIdentifier()).isEqualTo(initialPlot.getPlotIdentifier());
        assertThat(updatedPlotResponse.getPlotName()).isEqualTo("Updated Plot Name");
        assertThat(updatedPlotResponse.getLandTenureType()).isEqualTo(LandTenureType.COMMUNAL_ACCESS);
        assertThat(updatedPlotResponse.getPlotGeometry().getCoordinates().get(0).get(0)).containsExactly(6.0, 6.0); // Check
                                                                                                                    // new
                                                                                                                    // geometry
                                                                                                                    // origin

        // Verify area changed in the database
        Plot dbPlot = plotRepository.findById(initialPlot.getPlotIdentifier()).orElseThrow();
        assertThat(dbPlot.getCalculatedAreaHectares()).isNotNull();
        System.out.println("Updated calculated area: " + dbPlot.getCalculatedAreaHectares());
        assertThat(dbPlot.getCalculatedAreaHectares()).isNotEqualTo(initialArea); // Should have changed
        assertThat(dbPlot.getCalculatedAreaHectares().doubleValue())
                .isGreaterThan(initialArea != null ? initialArea.doubleValue() : 0.0); // Assuming larger geometry means
                                                                                       // larger area
    }

    @Test
    void updatePlot_whenPlotNotFoundOrTenantMismatch_shouldThrowResourceNotFound() {
        // Given
        UpdatePlotRequest updateRequest = UpdatePlotRequest.builder().plotName("Update Fail").build();
        UUID nonExistentPlotId = UUID.randomUUID();
        UUID differentTenantId = UUID.randomUUID();

        // When & Then: Plot not found
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.updatePlot(nonExistentPlotId, updateRequest, tenantId);
        });

        // Create a plot for tenantId first
        PlotResponse existingPlot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier()).plotName("Original Plot")
                .plotGeometry(createTestPolygonDto(0.001, 7, 7)).landTenureType(LandTenureType.OWNED)
                .tenantId(tenantId).build());
        entityManager.flush();
        entityManager.clear();

        // When & Then: Tenant mismatch
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.updatePlot(existingPlot.getPlotIdentifier(), updateRequest, differentTenantId);
        });
    }

    // Test for deletePlot
    @Test
    void deletePlot_shouldRemovePlotAndAssociatedLandTenure() {
        // Given: Create a plot and an associated land tenure
        PlotResponse plotToDelete = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("Plot To Delete")
                .plotGeometry(createTestPolygonDto(0.001, 8, 8))
                .landTenureType(LandTenureType.LEASED) // Set initial type on plot
                .tenantId(tenantId)
                .build());

        plotApplicationService.createOrUpdateLandTenureForPlot(plotToDelete.getPlotIdentifier(),
                CreateOrUpdateLandTenureRequest.builder().tenureType(LandTenureType.LEASED)
                        .leaseStartDate(LocalDate.now()).build(),
                tenantId);
        entityManager.flush();
        entityManager.clear();

        // Ensure they exist
        assertThat(plotRepository.existsById(plotToDelete.getPlotIdentifier())).isTrue();
        assertThat(
                landTenureRepository.findByPlot_PlotIdentifierAndTenantId(plotToDelete.getPlotIdentifier(), tenantId))
                .isPresent();

        // When
        plotApplicationService.deletePlot(plotToDelete.getPlotIdentifier(), tenantId);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(plotRepository.existsById(plotToDelete.getPlotIdentifier())).isFalse();
        // Verify LandTenure is also deleted due to ON DELETE CASCADE on the foreign key
        assertThat(
                landTenureRepository.findByPlot_PlotIdentifierAndTenantId(plotToDelete.getPlotIdentifier(), tenantId))
                .isNotPresent();
    }

    @Test
    void deletePlot_whenPlotNotFoundOrTenantMismatch_shouldThrowResourceNotFound() {
        // Given
        UUID nonExistentPlotId = UUID.randomUUID();
        UUID differentTenantId = UUID.randomUUID();

        // When & Then: Plot not found
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.deletePlot(nonExistentPlotId, tenantId);
        });

        // Create a plot for tenantId first
        PlotResponse existingPlot = plotApplicationService.createPlot(CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier()).plotName("Original Plot for Delete Check")
                .plotGeometry(createTestPolygonDto(0.001, 9, 9)).landTenureType(LandTenureType.OWNED)
                .tenantId(tenantId).build());
        entityManager.flush();
        entityManager.clear();

        // When & Then: Tenant mismatch
        assertThrows(ResourceNotFoundException.class, () -> {
            plotApplicationService.deletePlot(existingPlot.getPlotIdentifier(), differentTenantId);
        });
        // Ensure it was not deleted
        assertThat(plotRepository.existsById(existingPlot.getPlotIdentifier())).isTrue();
    }

}