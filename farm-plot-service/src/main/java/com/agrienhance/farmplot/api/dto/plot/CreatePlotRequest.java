package com.agrienhance.farmplot.api.dto.plot;

import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
// Assuming LandTenureType enum is in com.agrienhance.farmplot.domain.enums
import com.agrienhance.farmplot.domain.enums.LandTenureType;
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
@Schema(description = "Request payload for defining a new plot.")
public class CreatePlotRequest {

    @NotNull(message = "Farm identifier cannot be null.")
    @Schema(description = "UUID of the farm this plot belongs to.", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID farmIdentifier;

    @Size(max = 255, message = "Plot name cannot exceed 255 characters.")
    @Schema(description = "Optional name for the plot (e.g., 'North Field', 'Plot A1').", example = "Plot A1")
    private String plotName;

    @Schema(description = "UUID of the cultivator (user/entity) from IAM service assigned to this plot.", example = "b2c3d4e5-f6a7-8901-2345-67890abcdef")
    private UUID cultivatorReferenceId; // Optional on creation

    @NotNull(message = "Plot geometry cannot be null.")
    @Valid // Enable validation of nested PolygonGeometryDto
    @Schema(description = "Geospatial boundary of the plot as a GeoJSON Polygon.", requiredMode = Schema.RequiredMode.REQUIRED)
    private PolygonGeometryDto plotGeometry;

    // For simplicity, let's include a simplified LandTenureType directly.
    // A more complex LandTenure might have its own DTO and dedicated endpoints.
    @Schema(description = "Type of land tenure for the plot.", example = "OWNED")
    private LandTenureType landTenureType; // Using the enum

    // If LandTenureType.LEASED, these might be relevant:
    // @Schema(description = "Lease start date, if applicable (YYYY-MM-DD).",
    // example = "2025-01-01")
    // private String leaseStartDate; // Using String for simplicity, convert to
    // LocalDate in service
    // @Schema(description = "Lease end date, if applicable (YYYY-MM-DD).", example
    // = "2026-12-31")
    // private String leaseEndDate;

    // Will come from security context
    // @NotNull(message = "Tenant ID cannot be null.") // Should match the farm's
    // tenantId, usually validated in service
    // @Schema(description = "UUID of the tenant this plot belongs to.",
    // requiredMode = Schema.RequiredMode.REQUIRED)
    // private UUID tenantId;
}