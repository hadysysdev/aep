package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.api.dto.poi.UpdatePointOfInterestRequest;
import com.agrienhance.farmplot.domain.entity.PointOfInterest;
import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { PointOfInterestMapperImpl.class, GeometryMapper.class })
class PointOfInterestMapperTest {

    @Autowired
    private PointOfInterestMapper poiMapper;

    private GeometryFactory geometryFactory;
    private UUID parentId;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        parentId = UUID.randomUUID();
        tenantId = UUID.randomUUID();
    }

    private Point createTestPoint() {
        return geometryFactory.createPoint(new Coordinate(5.0, 15.0));
    }

    private PointGeometryDto createTestPointDto() {
        return PointGeometryDto.builder().type("Point").coordinates(List.of(5.0, 15.0)).build();
    }

    @Test
    void shouldMapCreateRequestToPoi() {
        // CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
        // .poiName("Water Pump")
        // .poiType(POIType.WATER_SOURCE)
        // .coordinates(createTestPointDto())
        // .build();

        // PointOfInterest poi = poiMapper.createRequestToPoi(request);

        // assertThat(poi).isNotNull();
        // assertThat(poi.getPoiName()).isEqualTo("Water Pump");
        // assertThat(poi.getPoiType()).isEqualTo(POIType.WATER_SOURCE);
        // assertThat(poi.getCoordinates().getX()).isEqualTo(5.0);
        // // assertThat(poi.getTenantId()).isEqualTo(tenantId);
        // assertThat(poi.getPoiIdentifier()).isNull(); // JPA generated

        // Given: CreatePointOfInterestRequest no longer has parentEntityIdentifier,
        // parentEntityType, or tenantId
        CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
                .poiName("Water Pump")
                .poiType(POIType.WATER_SOURCE)
                .coordinates(createTestPointDto()) // Assuming createTestPointDto() is a helper
                .notes("Primary water source for Plot B")
                .build(); // No parentId, parentType, tenantId here

        // When
        PointOfInterest poi = poiMapper.createRequestToPoi(request);

        // Then
        assertThat(poi).isNotNull();
        assertThat(poi.getPoiName()).isEqualTo("Water Pump");
        assertThat(poi.getPoiType()).isEqualTo(POIType.WATER_SOURCE);
        assertThat(poi.getCoordinates().getX()).isEqualTo(5.0); // Assuming createTestPointDto sets these
        assertThat(poi.getNotes()).isEqualTo("Primary water source for Plot B");

        // Assert that fields NOT in the simplified DTO and ignored by mapper are
        // null/default
        assertThat(poi.getPoiIdentifier()).isNull(); // Ignored, set by JPA
        assertThat(poi.getParentEntityIdentifier()).isNull(); // Ignored, to be set by service
        assertThat(poi.getParentEntityType()).isNull(); // Ignored, to be set by service
        assertThat(poi.getTenantId()).isNull(); // Ignored, to be set by service
        assertThat(poi.getCreatedAt()).isNull(); // Ignored, set by @PrePersist
        assertThat(poi.getUpdatedAt()).isNull(); // Ignored, set by @PrePersist
        assertThat(poi.getVersion()).isNull();

    }

    @Test
    void shouldMapPoiToPoiResponse() {
        PointOfInterest poi = PointOfInterest.builder()
                .poiIdentifier(UUID.randomUUID())
                .parentEntityIdentifier(parentId)
                .parentEntityType(ParentEntityType.FARM)
                .poiName("Main Gate")
                .poiType(POIType.ACCESS_POINT)
                .coordinates(createTestPoint())
                .notes("Primary access")
                .tenantId(tenantId)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .version(0L)
                .build();

        PointOfInterestResponse response = poiMapper.poiToPoiResponse(poi);

        assertThat(response).isNotNull();
        assertThat(response.getPoiIdentifier()).isEqualTo(poi.getPoiIdentifier());
        assertThat(response.getParentEntityIdentifier()).isEqualTo(parentId);
        assertThat(response.getParentEntityType()).isEqualTo(ParentEntityType.FARM);
        assertThat(response.getPoiName()).isEqualTo("Main Gate");
        assertThat(response.getCoordinates().getCoordinates()).containsExactly(5.0, 15.0);
    }

    @Test
    void shouldUpdatePoiFromRequest() {
        PointOfInterest existingPoi = PointOfInterest.builder()
                .poiIdentifier(UUID.randomUUID())
                .parentEntityIdentifier(parentId)
                .parentEntityType(ParentEntityType.PLOT)
                .poiName("Old POI Name")
                .poiType(POIType.OTHER)
                .coordinates(createTestPoint())
                .notes("Old notes")
                .tenantId(tenantId)
                .build();

        UpdatePointOfInterestRequest request = UpdatePointOfInterestRequest.builder()
                .poiName("New POI Name")
                .poiType(POIType.HAZARD)
                // coordinates can be updated as well
                .notes("New notes")
                .build();

        poiMapper.updatePoiFromRequest(request, existingPoi);

        assertThat(existingPoi.getPoiName()).isEqualTo("New POI Name");
        assertThat(existingPoi.getPoiType()).isEqualTo(POIType.HAZARD);
        assertThat(existingPoi.getNotes()).isEqualTo("New notes");
        // Ensure non-updatable fields are not changed
        assertThat(existingPoi.getParentEntityIdentifier()).isEqualTo(parentId);
    }
}