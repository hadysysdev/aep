package com.agrienhance.farmplot.api.dto.poi;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.domain.enums.POIType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// ParentEntityIdentifier, ParentEntityType, and TenantId are typically not updatable for an existing POI.
// If a POI needs to be moved, it's usually a delete & recreate.

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for updating an existing Point Of Interest. All fields are optional.")
public class UpdatePointOfInterestRequest {

    @Size(max = 255, message = "POI name cannot exceed 255 characters.")
    @Schema(description = "New name of the POI.", example = "Primary Well")
    private String poiName;

    @Schema(description = "New type of the POI.", example = "WATER_SOURCE")
    private POIType poiType; // Allow type update

    @Valid
    @Schema(description = "New coordinates of the POI as a GeoJSON Point.")
    private PointGeometryDto coordinates; // Allow location update

    @Schema(description = "Updated notes for the POI.")
    private String notes;
}