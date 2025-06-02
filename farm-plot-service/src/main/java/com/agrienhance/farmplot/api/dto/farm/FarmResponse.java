package com.agrienhance.farmplot.api.dto.farm;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing a farm resource.")
public class FarmResponse {

    @Schema(description = "Unique identifier of the farm.")
    private UUID farmIdentifier;

    @Schema(description = "Name of the farm.")
    private String farmName;

    @Schema(description = "UUID of the owner from IAM service.")
    private UUID ownerReferenceId;

    @Schema(description = "ISO 3166-1 alpha-2 country code.")
    private String countryCode;

    @Schema(description = "Administrative region within the country.")
    private String region;

    @Schema(description = "General location (centroid or main point) of the farm as GeoJSON Point_Legacy.")
    private PointGeometryDto generalLocationCoordinates;

    @Schema(description = "Descriptive notes about the farm.")
    private String notes;

    @Schema(description = "UUID of the tenant this farm belongs to.")
    private UUID tenantId;

    @Schema(description = "Timestamp of when the farm was created.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp of when the farm was last updated.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version number for optimistic locking.")
    private Long version;
}