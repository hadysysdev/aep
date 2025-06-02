package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.plot.CreatePlotRequest;
import com.agrienhance.farmplot.api.dto.plot.PlotResponse;
import com.agrienhance.farmplot.api.dto.plot.UpdatePlotRequest;
import com.agrienhance.farmplot.domain.entity.Farm; // Required for mapping farmIdentifier
import com.agrienhance.farmplot.domain.entity.Plot;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = { GeometryMapper.class }, // To handle PolygonGeometryDto <-> Polygon
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlotMapper {

    // Plot Entity from CreatePlotRequest
    // Note: Mapping 'farmIdentifier' from request to 'farm' (Farm object) in Plot
    // entity
    // is complex for MapStruct directly. Typically, the service layer fetches the
    // Farm entity
    // and sets it. So, we'll map other fields and handle 'farm' in the service.
    // Alternatively, we could have a method that accepts Farm as a parameter.
    @Mapping(source = "plotGeometry", target = "plotGeometry")
    @Mapping(source = "landTenureType", target = "landTenureType") // ADD THIS MAPPING
    @Mapping(target = "farm", ignore = true)
    @Mapping(target = "plotIdentifier", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "calculatedAreaHectares", ignore = true)
    Plot createRequestToPlot(CreatePlotRequest request);

    // PlotResponse DTO from Plot Entity
    @Mapping(source = "plotGeometry", target = "plotGeometry") // Uses GeometryMapper.toPolygonGeometryDto
    @Mapping(source = "farm.farmIdentifier", target = "farmIdentifier") // Map Farm object's ID to DTO field
    @Mapping(source = "landTenureType", target = "landTenureType") // ADD THIS MAPPING
    PlotResponse plotToPlotResponse(Plot plot);

    // Update existing Plot entity from UpdatePlotRequest
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(source = "plotGeometry", target = "plotGeometry") // Uses GeometryMapper.toPolygon
    @Mapping(source = "landTenureType", target = "landTenureType") // ADD THIS MAPPING
    @Mapping(target = "farm", ignore = true) // Farm is usually not changed this way
    @Mapping(target = "plotIdentifier", ignore = true)
    @Mapping(target = "tenantId", ignore = true) // Tenant ID should not change on update
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Will be set by @PreUpdate
    @Mapping(target = "version", ignore = true) // Will be handled by JPA
    @Mapping(target = "calculatedAreaHectares", ignore = true) // Recalculate if geometry changes
    void updatePlotFromRequest(UpdatePlotRequest request, @MappingTarget Plot plot);

    // Helper method if you need to pass Farm entity explicitly during mapping for
    // creation.
    // This provides an alternative to setting the farm in the service after
    // mapping.
    // default Plot createRequestToPlotWithFarm(CreatePlotRequest request, Farm
    // farm) {
    // Plot plot = createRequestToPlot(request); // Calls the main mapping method
    // if (plot != null) {
    // plot.setFarm(farm);
    // plot.setTenantId(farm.getTenantId()); // Ensure tenant consistency
    // }
    // return plot;
    // }
}