package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.api.dto.poi.UpdatePointOfInterestRequest;
import com.agrienhance.farmplot.domain.entity.PointOfInterest;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = { GeometryMapper.class }, // For PointGeometryDto <-> Point mapping
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PointOfInterestMapper {

    @Mapping(source = "coordinates", target = "coordinates") // Uses GeometryMapper
    PointOfInterestResponse poiToPoiResponse(PointOfInterest poi);

    @Mapping(source = "coordinates", target = "coordinates") // Uses GeometryMapper
    @Mapping(target = "poiIdentifier", ignore = true)
    @Mapping(target = "parentEntityIdentifier", ignore = true) // Will be set by service
    @Mapping(target = "parentEntityType", ignore = true) // Will be set by service
    @Mapping(target = "tenantId", ignore = true) // Will be set by service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    PointOfInterest createRequestToPoi(CreatePointOfInterestRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "coordinates", target = "coordinates") // Uses GeometryMapper
    @Mapping(target = "poiIdentifier", ignore = true)
    @Mapping(target = "parentEntityIdentifier", ignore = true) // Should not change
    @Mapping(target = "parentEntityType", ignore = true) // Should not change
    @Mapping(target = "tenantId", ignore = true) // Should not change
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updatePoiFromRequest(UpdatePointOfInterestRequest request, @MappingTarget PointOfInterest poi);
}