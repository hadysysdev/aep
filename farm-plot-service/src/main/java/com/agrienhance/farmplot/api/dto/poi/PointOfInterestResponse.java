package com.agrienhance.farmplot.api.dto.poi;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
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
@Schema(description = "Response payload representing a Point Of Interest.")
public class PointOfInterestResponse {

    @Schema(description = "Unique identifier of the POI.")
    private UUID poiIdentifier;

    @Schema(description = "Identifier of the parent entity (Farm or Plot) this POI belongs to.")
    private UUID parentEntityIdentifier;

    @Schema(description = "Type of the parent entity (FARM or PLOT).")
    private ParentEntityType parentEntityType;

    @Schema(description = "Name of the POI.")
    private String poiName;

    @Schema(description = "Type of the POI.")
    private POIType poiType;

    @Schema(description = "Coordinates of the POI as a GeoJSON Point.")
    private PointGeometryDto coordinates;

    @Schema(description = "Optional notes for the POI.")
    private String notes;

    @Schema(description = "UUID of the tenant this POI belongs to.")
    private UUID tenantId;

    @Schema(description = "Timestamp of when the POI was created.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp of when the POI was last updated.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version number for optimistic locking.")
    private Long version;
}