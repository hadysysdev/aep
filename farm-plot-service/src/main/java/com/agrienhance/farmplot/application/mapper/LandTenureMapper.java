package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.landtenure.CreateOrUpdateLandTenureRequest;
import com.agrienhance.farmplot.api.dto.landtenure.LandTenureResponse;
import com.agrienhance.farmplot.domain.entity.LandTenure;
import com.agrienhance.farmplot.domain.entity.Plot; // Needed for mapping plotIdentifier
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE) // Ignores unmapped target properties
public interface LandTenureMapper {

    @Mapping(source = "plot.plotIdentifier", target = "plotIdentifier")
    LandTenureResponse landTenureToLandTenureResponse(LandTenure landTenure);

    // For creating a new LandTenure entity.
    // The 'plot' entity itself and 'tenantId' will be set in the service layer.
    @Mapping(target = "landTenureIdentifier", ignore = true)
    @Mapping(target = "plot", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    LandTenure requestToLandTenure(CreateOrUpdateLandTenureRequest request);

    // For updating an existing LandTenure entity from a request.
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "landTenureIdentifier", ignore = true)
    @Mapping(target = "plot", ignore = true) // Plot association should not change via this update
    @Mapping(target = "tenantId", ignore = true) // Tenant ID should not change
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Will be set by @PreUpdate
    @Mapping(target = "version", ignore = true) // Will be handled by JPA
    void updateLandTenureFromRequest(CreateOrUpdateLandTenureRequest request, @MappingTarget LandTenure landTenure);

    // Helper or alternative mapping if you pass the Plot object directly
    // default LandTenure
    // requestToLandTenureWithPlot(CreateOrUpdateLandTenureRequest request, Plot
    // plot) {
    // if (request == null || plot == null) {
    // return null;
    // }
    // LandTenure tenure = requestToLandTenure(request); // Call the base mapping
    // tenure.setPlot(plot);
    // tenure.setTenantId(plot.getTenantId()); // Ensure consistency
    // return tenure;
    // }
}