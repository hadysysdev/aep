package com.agrienhance.farmplot.api.dto.plot;

import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Schema(description = "Request payload for updating an existing plot. All fields are optional.")
public class UpdatePlotRequest {

    // FarmIdentifier and TenantId are typically not changed on update of a plot.
    // If a plot needs to move to a different farm, it might be a delete &
    // re-create,
    // or a more complex "move plot" operation.

    @Size(max = 255, message = "Plot name cannot exceed 255 characters.")
    @Schema(description = "New name for the plot.", example = "Plot A1 - Irrigated")
    private String plotName;

    @Schema(description = "New UUID of the cultivator assigned to this plot.", example = "c3d4e5f6-a7b8-9012-3456-7890abcdef1")
    private UUID cultivatorReferenceId;

    @Valid // Enable validation of nested PolygonGeometryDto
    @Schema(description = "New geospatial boundary of the plot as a GeoJSON Polygon.")
    private PolygonGeometryDto plotGeometry;

    @Schema(description = "New type of land tenure for the plot.", example = "LEASED")
    private LandTenureType landTenureType;
}