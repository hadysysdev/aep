package com.agrienhance.farmplot.api.dto.farm;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload for creating a new farm.")
public class CreateFarmRequest {

    @NotBlank(message = "Farm name cannot be blank.")
    @Size(max = 255, message = "Farm name cannot exceed 255 characters.")
    @Schema(description = "Name of the farm.", example = "Green Valley Farm", requiredMode = Schema.RequiredMode.REQUIRED)
    private String farmName;

    @NotNull(message = "Owner reference ID cannot be null.")
    @Schema(description = "UUID of the owner (user/entity) from IAM service.", example = "a1b2c3d4-e5f6-7890-1234-567890abcdef", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID ownerReferenceId;

    @NotBlank(message = "Country code cannot be blank.")
    @Size(min = 2, max = 2, message = "Country code must be 2 characters.")
    @Schema(description = "ISO 3166-1 alpha-2 country code.", example = "KE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String countryCode;

    @Size(max = 255, message = "Region cannot exceed 255 characters.")
    @Schema(description = "Administrative region within the country.", example = "Rift Valley")
    private String region;

    @Valid // Enable validation of nested PointGeometryDto
    @Schema(description = "General location (centroid or main point) of the farm as GeoJSON Point_Legacy.")
    private PointGeometryDto generalLocationCoordinates;

    @Schema(description = "Optional descriptive notes about the farm.", example = "Fertile land, good water access.")
    private String notes;

    // Will come from security context
    // @NotNull(message = "Tenant ID cannot be null.")
    // Would come from security context
    // @Schema(description = "UUID of the tenant (cooperative/agribusiness) this
    // farm belongs to.", example = "f0e1d2c3-b4a5-6789-0123-456789abcdef",
    // requiredMode = Schema.RequiredMode.REQUIRED)
    // private UUID tenantId;
}