package com.agrienhance.farmplot.application.service;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.api.dto.poi.UpdatePointOfInterestRequest;
import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.PlotRepository;
import com.agrienhance.farmplot.domain.repository.PointOfInterestRepository;
import com.agrienhance.farmplot.application.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Transactional
public class PointOfInterestApplicationServiceIT extends AbstractIntegrationTest {

    @Autowired
    private PointOfInterestApplicationService poiApplicationService;

    @Autowired
    private PointOfInterestRepository poiRepository;

    @Autowired
    private PlotRepository plotRepository;

    @Autowired
    private FarmRepository farmRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Farm testFarm;
    private Plot testPlot;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // Clean up order is important
        poiRepository.deleteAllInBatch();
        plotRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();

        tenantId = UUID.randomUUID();
        testFarm = farmRepository.saveAndFlush(Farm.builder()
                .farmName("Farm for POI Service Tests")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("GH")
                .tenantId(tenantId)
                .build());

        Polygon plotGeom = createSimpleSquarePolygon(0.001, 0, 0);

        testPlot = plotRepository.saveAndFlush(Plot.builder()
                .farm(testFarm)
                .plotName("Plot for POI Service Tests")
                .plotGeometry(plotGeom)
                .tenantId(tenantId)
                .build());
    }

    @Test
    void createPoi_forFarmParent_shouldSucceed() {
        // Given
        CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
                .poiName("Farm Entrance Gate")
                .poiType(POIType.ACCESS_POINT)
                .coordinates(createTestPointDto(1.0, 1.0))
                .build();

        // When
        PointOfInterestResponse response = poiApplicationService.createPoi(
                testFarm.getFarmIdentifier(),
                ParentEntityType.FARM,
                tenantId,
                request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPoiIdentifier()).isNotNull();
        assertThat(response.getParentEntityIdentifier()).isEqualTo(testFarm.getFarmIdentifier());
        assertThat(response.getParentEntityType()).isEqualTo(ParentEntityType.FARM);
        assertThat(response.getPoiName()).isEqualTo("Farm Entrance Gate");
        assertThat(response.getTenantId()).isEqualTo(tenantId);
        assertThat(poiRepository.count()).isEqualTo(1);
    }

    @Test
    void createPoi_forPlotParent_shouldSucceed() {
        // Given
        CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
                .poiName("Plot Soil Sensor")
                .poiType(POIType.SOIL_SENSOR)
                .coordinates(createTestPointDto(0.0005, 0.0005))
                .build();

        // When
        PointOfInterestResponse response = poiApplicationService.createPoi(
                testPlot.getPlotIdentifier(),
                ParentEntityType.PLOT,
                tenantId,
                request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPoiIdentifier()).isNotNull();
        assertThat(response.getParentEntityIdentifier()).isEqualTo(testPlot.getPlotIdentifier());
        assertThat(response.getParentEntityType()).isEqualTo(ParentEntityType.PLOT);
        assertThat(response.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void createPoi_forNonExistentParent_shouldThrowResourceNotFoundException() {
        // Given
        UUID nonExistentParentId = UUID.randomUUID();
        CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
                .poiName("POI with bad parent")
                .poiType(POIType.OTHER)
                .coordinates(createTestPointDto(1.0, 1.0))
                .build();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            poiApplicationService.createPoi(nonExistentParentId, ParentEntityType.FARM, tenantId, request);
        });
    }

    @Test
    void createPoi_forParentInDifferentTenant_shouldThrowResourceNotFoundException() {
        // Given
        UUID otherTenantId = UUID.randomUUID();
        CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
                .poiName("POI with wrong tenant")
                .poiType(POIType.OTHER)
                .coordinates(createTestPointDto(1.0, 1.0))
                .build();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            // Attempt to create a POI for testFarm but providing wrong tenantId
            poiApplicationService.createPoi(testFarm.getFarmIdentifier(), ParentEntityType.FARM, otherTenantId,
                    request);
        });
    }

    @Test
    void listPoisByParent_shouldReturnCorrectPoisForPlot() {
        // Given
        poiApplicationService.createPoi(testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId,
                CreatePointOfInterestRequest.builder().poiName("POI 1 for Plot").poiType(POIType.HAZARD)
                        .coordinates(createTestPointDto(0.0002, 0.0002)).build());
        poiApplicationService.createPoi(testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId,
                CreatePointOfInterestRequest.builder().poiName("POI 2 for Plot").poiType(POIType.WATER_SOURCE)
                        .coordinates(createTestPointDto(0.0003, 0.0003)).build());
        // Create a POI for the farm, which should not be returned
        poiApplicationService.createPoi(testFarm.getFarmIdentifier(), ParentEntityType.FARM, tenantId,
                CreatePointOfInterestRequest.builder().poiName("POI for Farm").poiType(POIType.BUILDING)
                        .coordinates(createTestPointDto(0.0004, 0.0004)).build());
        entityManager.flush();
        entityManager.clear();

        // When
        List<PointOfInterestResponse> pois = poiApplicationService.listPoisByParent(
                testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId);

        // Then
        assertThat(pois).hasSize(2)
                .extracting(PointOfInterestResponse::getPoiName)
                .containsExactlyInAnyOrder("POI 1 for Plot", "POI 2 for Plot");
    }

    @Test
    void updatePoi_shouldChangeDetails() {
        // Given
        PointOfInterestResponse createdPoi = poiApplicationService.createPoi(testFarm.getFarmIdentifier(),
                ParentEntityType.FARM, tenantId,
                CreatePointOfInterestRequest.builder().poiName("Original Name").poiType(POIType.OTHER)
                        .coordinates(createTestPointDto(1.0, 1.0)).build());

        UpdatePointOfInterestRequest updateRequest = UpdatePointOfInterestRequest.builder()
                .poiName("Updated Name")
                .poiType(POIType.INFRASTRUCTURE)
                .notes("Updated notes.")
                .coordinates(createTestPointDto(1.1, 1.1))
                .build();

        // When
        PointOfInterestResponse updatedResponse = poiApplicationService.updatePoi(createdPoi.getPoiIdentifier(),
                updateRequest, tenantId);

        // Then
        assertThat(updatedResponse).isNotNull();
        assertThat(updatedResponse.getPoiIdentifier()).isEqualTo(createdPoi.getPoiIdentifier());
        assertThat(updatedResponse.getPoiName()).isEqualTo("Updated Name");
        assertThat(updatedResponse.getPoiType()).isEqualTo(POIType.INFRASTRUCTURE);
        assertThat(updatedResponse.getNotes()).isEqualTo("Updated notes.");
        assertThat(updatedResponse.getCoordinates().getCoordinates()).containsExactly(1.1, 1.1);
    }

    @Test
    void deletePoi_shouldRemovePoi() {
        // Given
        PointOfInterestResponse createdPoi = poiApplicationService.createPoi(testFarm.getFarmIdentifier(),
                ParentEntityType.FARM, tenantId,
                CreatePointOfInterestRequest.builder().poiName("To Be Deleted").poiType(POIType.OTHER)
                        .coordinates(createTestPointDto(1.0, 1.0)).build());
        assertThat(poiRepository.existsById(createdPoi.getPoiIdentifier())).isTrue();

        // When
        poiApplicationService.deletePoi(createdPoi.getPoiIdentifier(), tenantId);

        // Then
        assertThat(poiRepository.existsById(createdPoi.getPoiIdentifier())).isFalse();
    }

    @Test
    void listPoisByParentPaginated_shouldReturnCorrectPage() {
        // Given: Create 5 POIs for the same plot
        for (int i = 0; i < 5; i++) {
            poiApplicationService.createPoi(testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId,
                    CreatePointOfInterestRequest.builder()
                            .poiName("Paginated POI " + (i + 1))
                            .poiType(POIType.INFRASTRUCTURE)
                            .coordinates(createTestPointDto(0.0001 * i, 0.0001 * i))
                            .build());
        }
        entityManager.flush();
        entityManager.clear();

        // When: Request the first page with size 3, sorted by name
        Pageable pageRequest = PageRequest.of(0, 3, Sort.by("poiName").ascending());
        Page<PointOfInterestResponse> poiPage = poiApplicationService.listPoisByParentPaginated(
                testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId, pageRequest);

        // Then
        assertThat(poiPage).isNotNull();
        assertThat(poiPage.getTotalElements()).isEqualTo(5);
        assertThat(poiPage.getTotalPages()).isEqualTo(2);
        assertThat(poiPage.getNumberOfElements()).isEqualTo(3);
        assertThat(poiPage.getContent()).hasSize(3)
                .extracting(PointOfInterestResponse::getPoiName)
                .containsExactly("Paginated POI 1", "Paginated POI 2", "Paginated POI 3");

        // When: Request the second page
        pageRequest = PageRequest.of(1, 3, Sort.by("poiName").ascending());
        Page<PointOfInterestResponse> secondPoiPage = poiApplicationService.listPoisByParentPaginated(
                testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId, pageRequest);

        // Then
        assertThat(secondPoiPage.getTotalElements()).isEqualTo(5);
        assertThat(secondPoiPage.getNumberOfElements()).isEqualTo(2);
        assertThat(secondPoiPage.getContent()).hasSize(2)
                .extracting(PointOfInterestResponse::getPoiName)
                .containsExactly("Paginated POI 4", "Paginated POI 5");
    }

    @Test
    void listPoisByParent_whenParentExistsButHasNoPois_shouldReturnEmptyList() {
        // Given: A testPlot with no POIs is already created in setUp()

        // When
        List<PointOfInterestResponse> pois = poiApplicationService.listPoisByParent(
                testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId);

        // Then
        assertThat(pois).isNotNull().isEmpty();
    }

    @Test
    void updatePoi_whenPoiNotFound_shouldThrowResourceNotFoundException() {
        // Given
        UUID nonExistentPoiId = UUID.randomUUID();
        UpdatePointOfInterestRequest updateRequest = UpdatePointOfInterestRequest.builder()
                .poiName("This update will fail")
                .build();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            poiApplicationService.updatePoi(nonExistentPoiId, updateRequest, tenantId);
        });
    }

    @Test
    void updatePoi_whenTenantMismatch_shouldThrowResourceNotFoundException() {
        // Given
        PointOfInterestResponse createdPoi = poiApplicationService.createPoi(testFarm.getFarmIdentifier(),
                ParentEntityType.FARM, tenantId,
                CreatePointOfInterestRequest.builder().poiName("Original Name").poiType(POIType.OTHER)
                        .coordinates(createTestPointDto(1.0, 1.0)).build());

        UpdatePointOfInterestRequest updateRequest = UpdatePointOfInterestRequest.builder()
                .poiName("This update will also fail")
                .build();

        UUID otherTenantId = UUID.randomUUID();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            poiApplicationService.updatePoi(createdPoi.getPoiIdentifier(), updateRequest, otherTenantId);
        });
    }

    @Test
    void deletePoi_whenPoiNotFound_shouldThrowResourceNotFoundException() {
        // Given
        UUID nonExistentPoiId = UUID.randomUUID();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            poiApplicationService.deletePoi(nonExistentPoiId, tenantId);
        });
    }

    @Test
    void deletePoi_whenTenantMismatch_shouldThrowResourceNotFoundException() {
        // Given
        PointOfInterestResponse createdPoi = poiApplicationService.createPoi(testFarm.getFarmIdentifier(),
                ParentEntityType.FARM, tenantId,
                CreatePointOfInterestRequest.builder().poiName("POI To Protect").poiType(POIType.OTHER)
                        .coordinates(createTestPointDto(1.0, 1.0)).build());

        UUID otherTenantId = UUID.randomUUID();

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            poiApplicationService.deletePoi(createdPoi.getPoiIdentifier(), otherTenantId);
        });

        // Assert that the POI was NOT deleted
        assertThat(poiRepository.existsById(createdPoi.getPoiIdentifier())).isTrue();
    }

}