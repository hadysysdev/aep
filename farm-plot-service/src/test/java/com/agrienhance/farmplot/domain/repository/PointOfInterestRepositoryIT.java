package com.agrienhance.farmplot.domain.repository;

import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.entity.PointOfInterest;
import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
public class PointOfInterestRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private PointOfInterestRepository poiRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private PlotRepository plotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Farm testFarm;
    private Plot testPlot;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        // Clean up order matters
        poiRepository.deleteAllInBatch();
        plotRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();
        entityManager.flush();
        entityManager.clear();

        tenantId = UUID.randomUUID();
        testFarm = Farm.builder()
                .farmName("Farm for POI Tests")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("PI") // POI Test Country
                .tenantId(tenantId)
                .build();
        farmRepository.saveAndFlush(testFarm);

        // Use a much smaller polygon, e.g., 0.001 x 0.001 degrees
        double side = 0.001; // Or similar small value like 0.0001
        double originX = 0.0;
        double originY = 0.0;

        Polygon plotGeom = createSimpleSquarePolygon(side, originX, originY);

        testPlot = Plot.builder()
                .farm(testFarm)
                .plotName("Plot for POI Tests")
                .plotGeometry(plotGeom)
                .tenantId(testFarm.getTenantId())
                .build();
        plotRepository.saveAndFlush(testPlot);
    }

    // No @AfterEach needed if @Transactional is handling rollback and @BeforeEach
    // handles cleanup

    @Test
    void shouldSaveAndRetrievePoiForFarm() {
        // Given
        Point poiCoords = createJtsPoint(10.1, 20.1);
        PointOfInterest poi = PointOfInterest.builder()
                .parentEntityIdentifier(testFarm.getFarmIdentifier())
                .parentEntityType(ParentEntityType.FARM)
                .poiName("Farm Well")
                .poiType(POIType.WATER_SOURCE)
                .coordinates(poiCoords)
                .tenantId(testFarm.getTenantId())
                .build();

        // When
        PointOfInterest savedPoi = poiRepository.save(poi);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<PointOfInterest> foundOpt = poiRepository.findById(savedPoi.getPoiIdentifier());
        assertThat(foundOpt).isPresent();
        PointOfInterest found = foundOpt.get();

        assertThat(found.getPoiName()).isEqualTo("Farm Well");
        assertThat(found.getParentEntityIdentifier()).isEqualTo(testFarm.getFarmIdentifier());
        assertThat(found.getParentEntityType()).isEqualTo(ParentEntityType.FARM);
        assertThat(found.getTenantId()).isEqualTo(testFarm.getTenantId());
        assertThat(found.getCoordinates().equalsExact(poiCoords, 0.00001)).isTrue();
        assertThat(found.getVersion()).isNotNull().isEqualTo(0L);
    }

    @Test
    void shouldSaveAndRetrievePoiForPlot() {
        // Given
        Point poiCoords = createJtsPoint(0.0005, 0.0005); // Within the testPlot geometry
        PointOfInterest poi = PointOfInterest.builder()
                .parentEntityIdentifier(testPlot.getPlotIdentifier())
                .parentEntityType(ParentEntityType.PLOT)
                .poiName("Plot Soil Sensor")
                .poiType(POIType.SOIL_SENSOR)
                .coordinates(poiCoords)
                .tenantId(testPlot.getTenantId())
                .build();

        // When
        PointOfInterest savedPoi = poiRepository.save(poi);
        entityManager.flush();
        entityManager.clear();

        // Then
        Optional<PointOfInterest> foundOpt = poiRepository.findById(savedPoi.getPoiIdentifier());
        assertThat(foundOpt).isPresent();
        PointOfInterest found = foundOpt.get();

        assertThat(found.getPoiName()).isEqualTo("Plot Soil Sensor");
        assertThat(found.getParentEntityIdentifier()).isEqualTo(testPlot.getPlotIdentifier());
        assertThat(found.getParentEntityType()).isEqualTo(ParentEntityType.PLOT);
    }

    @Test
    void findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId_shouldReturnPoisForFarm() {
        // Given
        poiRepository.save(PointOfInterest.builder().parentEntityIdentifier(testFarm.getFarmIdentifier())
                .parentEntityType(ParentEntityType.FARM)
                .poiName("Farm POI 1").poiType(POIType.BUILDING).coordinates(createJtsPoint(1, 1)).tenantId(tenantId)
                .build());
        poiRepository.save(PointOfInterest.builder().parentEntityIdentifier(testFarm.getFarmIdentifier())
                .parentEntityType(ParentEntityType.FARM)
                .poiName("Farm POI 2").poiType(POIType.ACCESS_POINT).coordinates(createJtsPoint(2, 2))
                .tenantId(tenantId).build());
        // POI for a plot (should not be returned)
        poiRepository.save(PointOfInterest.builder().parentEntityIdentifier(testPlot.getPlotIdentifier())
                .parentEntityType(ParentEntityType.PLOT)
                .poiName("Plot POI").poiType(POIType.OTHER).coordinates(createJtsPoint(3, 3)).tenantId(tenantId)
                .build());
        entityManager.flush();
        entityManager.clear();

        // When
        List<PointOfInterest> farmPois = poiRepository.findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId(
                testFarm.getFarmIdentifier(), ParentEntityType.FARM, tenantId);

        // Then
        assertThat(farmPois).hasSize(2)
                .extracting(PointOfInterest::getPoiName)
                .containsExactlyInAnyOrder("Farm POI 1", "Farm POI 2");
    }

    @Test
    void findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId_paginated_shouldWork() {
        // Given
        for (int i = 0; i < 5; i++) {
            poiRepository.save(PointOfInterest.builder().parentEntityIdentifier(testPlot.getPlotIdentifier())
                    .parentEntityType(ParentEntityType.PLOT)
                    .poiName("Plot POI " + i).poiType(POIType.OTHER).coordinates(createJtsPoint(0.0001 * i, 0.0001 * i))
                    .tenantId(tenantId).build());
        }
        entityManager.flush();
        entityManager.clear();

        // When
        Page<PointOfInterest> poiPage = poiRepository.findAllByParentEntityIdentifierAndParentEntityTypeAndTenantId(
                testPlot.getPlotIdentifier(), ParentEntityType.PLOT, tenantId, PageRequest.of(0, 3));

        // Then
        assertThat(poiPage.getTotalElements()).isEqualTo(5);
        assertThat(poiPage.getContent()).hasSize(3);
        assertThat(poiPage.getNumber()).isEqualTo(0);
        assertThat(poiPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findPoisWithinGeometry_shouldReturnCorrectPois() {
        // Given
        Point poi1Coords = createJtsPoint(10.0001, 10.0001); // Inside query rect
        PointOfInterest poi1 = createAndSavePoiForParent("POI Inside", poi1Coords, testFarm.getFarmIdentifier(),
                ParentEntityType.FARM, tenantId);

        Point poi2Coords = createJtsPoint(10.0006, 10.0006); // Inside query rect
        PointOfInterest poi2 = createAndSavePoiForParent("POI Also Inside", poi2Coords, testPlot.getPlotIdentifier(),
                ParentEntityType.PLOT, tenantId);

        Point poi3Coords = createJtsPoint(20.0, 20.0); // Outside query rect
        createAndSavePoiForParent("POI Outside", poi3Coords, testFarm.getFarmIdentifier(), ParentEntityType.FARM,
                tenantId);

        // Query rectangle from (10.0, 10.0) with side 0.001
        Polygon queryPolygon = geometryFactory.createPolygon(new Coordinate[] {
                new Coordinate(10.0, 10.0), new Coordinate(10.001, 10.0),
                new Coordinate(10.001, 10.001), new Coordinate(10.0, 10.001),
                new Coordinate(10.0, 10.0)
        });
        entityManager.flush();
        entityManager.clear();

        // When
        List<PointOfInterest> poisWithin = poiRepository.findPoisWithinGeometry(tenantId, queryPolygon);

        // Then
        assertThat(poisWithin).hasSize(2)
                .extracting(PointOfInterest::getPoiIdentifier)
                .containsExactlyInAnyOrder(poi1.getPoiIdentifier(), poi2.getPoiIdentifier());
    }

    // Helper to quickly create and save POIs for tests
    private PointOfInterest createAndSavePoiForParent(String name, Point coords, UUID parentId,
            ParentEntityType parentType, UUID tenantId) {
        PointOfInterest poi = PointOfInterest.builder()
                .parentEntityIdentifier(parentId)
                .parentEntityType(parentType)
                .poiName(name)
                .poiType(POIType.OTHER)
                .coordinates(coords)
                .tenantId(tenantId)
                .build();
        return poiRepository.saveAndFlush(poi);
    }
}