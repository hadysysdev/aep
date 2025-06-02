package com.agrienhance.farmplot.application.service.impl;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.application.exception.ResourceNotFoundException;
import com.agrienhance.farmplot.application.mapper.PointOfInterestMapper;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.entity.PointOfInterest;
import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.PlotRepository;
import com.agrienhance.farmplot.domain.repository.PointOfInterestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointOfInterestApplicationServiceImplTest {

    @Mock
    private PointOfInterestRepository poiRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private PlotRepository plotRepository;
    @Mock
    private PointOfInterestMapper poiMapper;

    @InjectMocks
    private PointOfInterestApplicationServiceImpl poiApplicationService;

    private UUID tenantId;
    private UUID parentFarmId;
    private UUID parentPlotId;
    private CreatePointOfInterestRequest createPoiRequest;
    private PointOfInterest poiFromMapper;
    private PointOfInterest savedPoi;
    private PointOfInterestResponse poiResponseFromMapper;
    private GeometryFactory geometryFactory;
    private Point testJtsPoint;
    private PointGeometryDto testPointDto;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        tenantId = UUID.randomUUID();
        parentFarmId = UUID.randomUUID();
        parentPlotId = UUID.randomUUID();

        testJtsPoint = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
        testPointDto = PointGeometryDto.builder().type("Point").coordinates(List.of(10.0, 20.0)).build();

        // Simplified DTO (no parent/tenant info)
        createPoiRequest = CreatePointOfInterestRequest.builder()
                .poiName("Test POI")
                .poiType(POIType.WATER_SOURCE)
                .coordinates(testPointDto)
                .notes("A test POI")
                .build();

        // What mapper returns from simplified DTO (parent/tenant IDs are still null)
        poiFromMapper = new PointOfInterest();
        poiFromMapper.setPoiName(createPoiRequest.getPoiName());
        poiFromMapper.setPoiType(createPoiRequest.getPoiType());
        poiFromMapper.setCoordinates(testJtsPoint); // Assume mapper calls GeometryMapper
        poiFromMapper.setNotes(createPoiRequest.getNotes());

        // What repository.save returns (all fields set)
        savedPoi = new PointOfInterest();
        savedPoi.setPoiIdentifier(UUID.randomUUID());
        savedPoi.setParentEntityIdentifier(parentFarmId); // Example for FARM parent
        savedPoi.setParentEntityType(ParentEntityType.FARM);
        savedPoi.setTenantId(tenantId);
        savedPoi.setPoiName(createPoiRequest.getPoiName());
        savedPoi.setPoiType(createPoiRequest.getPoiType());
        savedPoi.setCoordinates(testJtsPoint);
        savedPoi.setNotes(createPoiRequest.getNotes());
        savedPoi.setCreatedAt(OffsetDateTime.now());
        savedPoi.setUpdatedAt(OffsetDateTime.now());
        savedPoi.setVersion(1L);

        // What mapper returns for response
        poiResponseFromMapper = PointOfInterestResponse.builder()
                .poiIdentifier(savedPoi.getPoiIdentifier())
                .parentEntityIdentifier(savedPoi.getParentEntityIdentifier())
                .parentEntityType(savedPoi.getParentEntityType())
                .tenantId(savedPoi.getTenantId())
                .poiName(savedPoi.getPoiName())
                .poiType(savedPoi.getPoiType())
                .coordinates(testPointDto)
                .notes(savedPoi.getNotes())
                .createdAt(savedPoi.getCreatedAt())
                .updatedAt(savedPoi.getUpdatedAt())
                .version(savedPoi.getVersion())
                .build();
    }

    @Test
    void createPoi_forFarmParent_shouldSucceedAndSetParentAndTenantInfo() {
        // Arrange
        // Mock mapper for DTO -> Entity (initial mapping)
        when(poiMapper.createRequestToPoi(createPoiRequest)).thenReturn(poiFromMapper);

        // Mock parent validation (Farm exists)
        when(farmRepository.findByFarmIdentifierAndTenantId(parentFarmId, tenantId))
                .thenReturn(Optional.of(new Farm())); // Return dummy farm

        // Mock repository save: use ArgumentCaptor to capture the entity passed to save
        ArgumentCaptor<PointOfInterest> poiCaptor = ArgumentCaptor.forClass(PointOfInterest.class);
        when(poiRepository.save(poiCaptor.capture())).thenReturn(savedPoi); // Return the fully fleshed savedPoi

        // Mock mapper for Entity -> Response DTO
        when(poiMapper.poiToPoiResponse(savedPoi)).thenReturn(poiResponseFromMapper);

        // Act
        PointOfInterestResponse actualResponse = poiApplicationService.createPoi(
                parentFarmId, ParentEntityType.FARM, tenantId, createPoiRequest);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getPoiIdentifier()).isEqualTo(savedPoi.getPoiIdentifier());
        assertThat(actualResponse.getParentEntityIdentifier()).isEqualTo(parentFarmId);
        assertThat(actualResponse.getParentEntityType()).isEqualTo(ParentEntityType.FARM);
        assertThat(actualResponse.getTenantId()).isEqualTo(tenantId);
        assertThat(actualResponse.getPoiName()).isEqualTo(createPoiRequest.getPoiName());

        // Verify interactions and captured argument
        verify(farmRepository).findByFarmIdentifierAndTenantId(parentFarmId, tenantId);
        verify(plotRepository, never()).findByPlotIdentifierAndTenantId(any(), any()); // Ensure plot repo not called
                                                                                       // for FARM parent
        verify(poiMapper).createRequestToPoi(createPoiRequest);
        verify(poiRepository).save(any(PointOfInterest.class)); // Or poiCaptor.capture() is enough
        verify(poiMapper).poiToPoiResponse(savedPoi);

        PointOfInterest capturedPoi = poiCaptor.getValue();
        assertThat(capturedPoi.getParentEntityIdentifier()).isEqualTo(parentFarmId);
        assertThat(capturedPoi.getParentEntityType()).isEqualTo(ParentEntityType.FARM);
        assertThat(capturedPoi.getTenantId()).isEqualTo(tenantId);
        assertThat(capturedPoi.getPoiName()).isEqualTo(createPoiRequest.getPoiName()); // Check other fields mapped from
                                                                                       // DTO
    }

    @Test
    void createPoi_forPlotParent_shouldSucceed() {
        // Arrange
        savedPoi.setParentEntityIdentifier(parentPlotId); // Adjust savedPoi for this test case
        savedPoi.setParentEntityType(ParentEntityType.PLOT);
        poiResponseFromMapper.setParentEntityIdentifier(parentPlotId); // Adjust response for this test case
        poiResponseFromMapper.setParentEntityType(ParentEntityType.PLOT);

        when(poiMapper.createRequestToPoi(createPoiRequest)).thenReturn(poiFromMapper);
        when(plotRepository.findByPlotIdentifierAndTenantId(parentPlotId, tenantId))
                .thenReturn(Optional.of(new Plot())); // Mock Plot parent validation
        ArgumentCaptor<PointOfInterest> poiCaptor = ArgumentCaptor.forClass(PointOfInterest.class);
        when(poiRepository.save(poiCaptor.capture())).thenReturn(savedPoi);
        when(poiMapper.poiToPoiResponse(savedPoi)).thenReturn(poiResponseFromMapper);

        // Act
        PointOfInterestResponse actualResponse = poiApplicationService.createPoi(
                parentPlotId, ParentEntityType.PLOT, tenantId, createPoiRequest);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getParentEntityIdentifier()).isEqualTo(parentPlotId);
        assertThat(actualResponse.getParentEntityType()).isEqualTo(ParentEntityType.PLOT);

        verify(plotRepository).findByPlotIdentifierAndTenantId(parentPlotId, tenantId);
        verify(farmRepository, never()).findByFarmIdentifierAndTenantId(any(), any()); // Ensure farm repo not called

        PointOfInterest capturedPoi = poiCaptor.getValue();
        assertThat(capturedPoi.getParentEntityIdentifier()).isEqualTo(parentPlotId);
        assertThat(capturedPoi.getParentEntityType()).isEqualTo(ParentEntityType.PLOT);
        assertThat(capturedPoi.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void createPoi_whenParentFarmNotFound_shouldThrowResourceNotFoundException() {
        // Arrange
        when(farmRepository.findByFarmIdentifierAndTenantId(parentFarmId, tenantId))
                .thenReturn(Optional.empty()); // Parent farm does not exist

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            poiApplicationService.createPoi(parentFarmId, ParentEntityType.FARM, tenantId, createPoiRequest);
        });

        assertThat(exception.getMessage()).contains("Farm with identifier [" + parentFarmId.toString());
        verify(poiRepository, never()).save(any()); // Ensure save is not called
    }

    // Similar test for when Parent PLOT is not found can be added
}