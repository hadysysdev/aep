package com.agrienhance.farmplot.api.dto.farm;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Note: No UUID for ownerReferenceId or tenantId here, assuming they are not updated via this DTO
// or are handled by more specific service methods/endpoints.

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for updating an existing farm. All fields are optional.")
public class UpdateFarmRequest {

    @Size(max = 255, message = "Farm name cannot exceed 255 characters.")
    @Schema(description = "New name of the farm.", example = "Green Valley Farm Deluxe")
    private String farmName;

    // countryCode and region might be updatable
    @Size(min = 2, max = 2, message = "Country code must be 2 characters.")
    @Schema(description = "New ISO 3166-1 alpha-2 country code.", example = "UG")
    private String countryCode;

    @Size(max = 255, message = "Region cannot exceed 255 characters.")
    @Schema(description = "New administrative region within the country.", example = "Central Region")
    private String region;

    @Valid
    @Schema(description = "New general location (centroid or main point) of the farm as GeoJSON Point_Legacy.")
    private PointGeometryDto generalLocationCoordinates;

    @Schema(description = "Updated descriptive notes about the farm.", example = "Expanded irrigation system.")
    private String notes;
}