package com.agrienhance.farmplot.application.service.impl;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.FarmResponse;
import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.application.mapper.FarmMapper;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.any; // For any(Farm.class) etc.
import static org.mockito.Mockito.*; // For when, verify

@ExtendWith(MockitoExtension.class) // Initializes mocks and injects them
class FarmApplicationServiceImplTest {

    @Mock // Mockito will create a mock instance of FarmRepository
    private FarmRepository farmRepository;

    @Mock // Mockito will create a mock instance of FarmMapper
    private FarmMapper farmMapper;

    @InjectMocks // Mockito will inject the mocks above into this instance
    private FarmApplicationServiceImpl farmApplicationService;

    private CreateFarmRequest createFarmRequest;
    private Farm farmEntityFromMapper;
    private Farm savedFarmEntity;
    private FarmResponse farmResponseFromMapper;
    private GeometryFactory geometryFactory;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        tenantId = UUID.randomUUID(); // Simulate a tenant ID for the tests
        // 1. Prepare input DTO
        PointGeometryDto pointDto = PointGeometryDto.builder()
                .type("Point")
                .coordinates(List.of(10.0, 20.0))
                .build();
        createFarmRequest = CreateFarmRequest.builder()
                .farmName("Test Service Farm")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("SV")
                .generalLocationCoordinates(pointDto)
                .build();

        // 2. Prepare what FarmMapper.createRequestToFarm would return
        Point farmPoint = geometryFactory.createPoint(new Coordinate(10.0, 20.0));
        farmEntityFromMapper = new Farm(); // Using new, but could use Farm.builder() if desired for setup
        farmEntityFromMapper.setFarmName(createFarmRequest.getFarmName());
        farmEntityFromMapper.setOwnerReferenceId(createFarmRequest.getOwnerReferenceId());
        farmEntityFromMapper.setCountryCode(createFarmRequest.getCountryCode());
        farmEntityFromMapper.setTenantId(tenantId);
        farmEntityFromMapper.setGeneralLocationCoordinates(farmPoint);
        // JPA/Lifecycle callbacks would set ID, timestamps, version upon save

        // 3. Prepare what FarmRepository.save would return
        savedFarmEntity = new Farm();
        savedFarmEntity.setFarmIdentifier(UUID.randomUUID()); // ID is set after save
        savedFarmEntity.setFarmName(farmEntityFromMapper.getFarmName());
        savedFarmEntity.setOwnerReferenceId(farmEntityFromMapper.getOwnerReferenceId());
        savedFarmEntity.setCountryCode(farmEntityFromMapper.getCountryCode());
        savedFarmEntity.setTenantId(farmEntityFromMapper.getTenantId());
        savedFarmEntity.setGeneralLocationCoordinates(farmEntityFromMapper.getGeneralLocationCoordinates());
        savedFarmEntity.setCreatedAt(OffsetDateTime.now());
        savedFarmEntity.setUpdatedAt(OffsetDateTime.now());
        savedFarmEntity.setVersion(1L);

        // 4. Prepare what FarmMapper.farmToFarmResponse would return
        farmResponseFromMapper = FarmResponse.builder()
                .farmIdentifier(savedFarmEntity.getFarmIdentifier())
                .farmName(savedFarmEntity.getFarmName())
                .ownerReferenceId(savedFarmEntity.getOwnerReferenceId())
                .countryCode(savedFarmEntity.getCountryCode())
                .tenantId(savedFarmEntity.getTenantId())
                .generalLocationCoordinates(pointDto) // Mapper would convert Point back to PointGeometryDto
                .createdAt(savedFarmEntity.getCreatedAt())
                .updatedAt(savedFarmEntity.getUpdatedAt())
                .version(savedFarmEntity.getVersion())
                .build();
    }

    @Test
    void createFarm_shouldSaveAndReturnFarmResponse() {
        // Given (Arrange - Mock behaviors)
        when(farmMapper.createRequestToFarm(createFarmRequest)).thenReturn(farmEntityFromMapper);
        when(farmRepository.save(any(Farm.class))).thenReturn(savedFarmEntity); // farmEntityFromMapper will have
                                                                                // timestamps updated by @PrePersist
        when(farmMapper.farmToFarmResponse(savedFarmEntity)).thenReturn(farmResponseFromMapper);

        // When (Act)
        FarmResponse actualResponse = farmApplicationService.createFarm(createFarmRequest, tenantId);

        // Then (Assert)
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getFarmIdentifier()).isEqualTo(savedFarmEntity.getFarmIdentifier());
        assertThat(actualResponse.getFarmName()).isEqualTo(createFarmRequest.getFarmName());
        assertThat(actualResponse.getTenantId()).isEqualTo(tenantId);

        // Verify interactions
        verify(farmMapper).createRequestToFarm(createFarmRequest);
        // We need to be careful here. The entity passed to save will have timestamps
        // set by @PrePersist.
        // For simplicity, we can verify that save was called with the object that
        // createRequestToFarm returned.
        // Or use an ArgumentCaptor if we need to inspect the saved object precisely.
        verify(farmRepository).save(farmEntityFromMapper);
        verify(farmMapper).farmToFarmResponse(savedFarmEntity);

        verifyNoMoreInteractions(farmMapper, farmRepository); // Ensure no other unexpected calls
    }

    // Example test for getFarmById - success case
    @Test
    void getFarmById_whenFarmExists_shouldReturnFarmResponse() {
        // Given
        UUID farmId = savedFarmEntity.getFarmIdentifier();
        UUID tenantId = savedFarmEntity.getTenantId();
        when(farmRepository.findByFarmIdentifierAndTenantId(farmId, tenantId)).thenReturn(Optional.of(savedFarmEntity));
        when(farmMapper.farmToFarmResponse(savedFarmEntity)).thenReturn(farmResponseFromMapper);

        // When
        FarmResponse actualResponse = farmApplicationService.getFarmById(farmId, tenantId);

        // Then
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getFarmIdentifier()).isEqualTo(farmId);

        verify(farmRepository).findByFarmIdentifierAndTenantId(farmId, tenantId);
        verify(farmMapper).farmToFarmResponse(savedFarmEntity);
        verifyNoMoreInteractions(farmRepository, farmMapper);
    }

    // We would also add a test for getFarmById when farm does NOT exist to check
    // ResourceNotFoundException
    // e.g., using assertThrows(ResourceNotFoundException.class, () ->
    // farmApplicationService.getFarmById(...));
}