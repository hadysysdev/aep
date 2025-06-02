package com.agrienhance.farmplot.api.dto.poi;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating a new Point Of Interest.")
public class CreatePointOfInterestRequest {

    // parentEntityIdentifier will usually be part of the path, e.g.,
    // /farms/{farmId}/pois
    // For this DTO, we'll keep it to allow for a generic /pois endpoint if desired,
    // but sub-resource endpoints are cleaner. Let's assume it might be needed for a
    // generic service method.
    // @NotNull(message = "Parent entity identifier cannot be null.")
    // @Schema(description = "UUID of the parent entity (Farm or Plot).",
    // requiredMode = Schema.RequiredMode.REQUIRED)
    // private UUID parentEntityIdentifier;

    // tenantId will usually be part of the path, e.g.,
    // @NotNull(message = "Parent entity type cannot be null.")
    // @Schema(description = "Type of the parent entity (FARM or PLOT).",
    // requiredMode = Schema.RequiredMode.REQUIRED)
    // private ParentEntityType parentEntityType;

    @Size(max = 255, message = "POI name cannot exceed 255 characters.")
    @Schema(description = "Name of the POI.", example = "Main Well")
    private String poiName;

    @NotNull(message = "POI type cannot be null.")
    @Schema(description = "Type of the POI.", requiredMode = Schema.RequiredMode.REQUIRED, example = "WATER_SOURCE")
    private POIType poiType;

    @NotNull(message = "Coordinates cannot be null.")
    @Valid
    @Schema(description = "Coordinates of the POI as a GeoJSON Point.", requiredMode = Schema.RequiredMode.REQUIRED)
    private PointGeometryDto coordinates;

    @Schema(description = "Optional notes for the POI.")
    private String notes;

    // @NotNull(message = "Tenant ID cannot be null.") // Should match parent's
    // tenantId, validated in service
    // @Schema(description = "UUID of the tenant this POI belongs to.", requiredMode
    // = Schema.RequiredMode.REQUIRED)
    // private UUID tenantId;
}