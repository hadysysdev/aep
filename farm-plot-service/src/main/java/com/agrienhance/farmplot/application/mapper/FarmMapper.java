package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.FarmResponse;
import com.agrienhance.farmplot.api.dto.farm.UpdateFarmRequest;
import com.agrienhance.farmplot.domain.entity.Farm;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", // Generates a Spring bean for the mapper
        uses = { GeometryMapper.class }, // Tells MapStruct it can use GeometryMapper
        unmappedTargetPolicy = ReportingPolicy.IGNORE) // Optional: Ignores unmapped target properties
public interface FarmMapper {

    // Farm Entity from CreateFarmRequest
    // Timestamps (createdAt, updatedAt, version) and farmIdentifier are usually set
    // by JPA/DB or in service
    @Mapping(source = "generalLocationCoordinates", target = "generalLocationCoordinates") // Uses
                                                                                           // GeometryMapper.toPoint
    Farm createRequestToFarm(CreateFarmRequest request);

    // FarmResponse DTO from Farm Entity
    @Mapping(source = "generalLocationCoordinates", target = "generalLocationCoordinates") // Uses
                                                                                           // GeometryMapper.toPointGeometryDto
    FarmResponse farmToFarmResponse(Farm farm);

    // Update existing Farm entity from UpdateFarmRequest
    // This will update only non-null fields from the request DTO onto the farm
    // entity
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "generalLocationCoordinates", target = "generalLocationCoordinates") // Uses
                                                                                           // GeometryMapper.toPoint
    void updateFarmFromRequest(UpdateFarmRequest request, @MappingTarget Farm farm);
}