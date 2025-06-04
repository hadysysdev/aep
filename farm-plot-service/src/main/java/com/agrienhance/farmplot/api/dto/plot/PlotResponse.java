package com.agrienhance.farmplot.api.dto.plot;

import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response payload representing a plot resource.")
public class PlotResponse {

    @Schema(description = "Unique identifier of the plot.")
    private UUID plotIdentifier;

    @Schema(description = "UUID of the farm this plot belongs to.")
    private UUID farmIdentifier; // Or include a nested FarmSummaryDto

    @Schema(description = "Name of the plot.")
    private String plotName;

    @Schema(description = "UUID of the cultivator assigned to this plot.")
    private UUID cultivatorReferenceId;

    @Schema(description = "Geospatial boundary of the plot as a GeoJSON Polygon.")
    private PolygonGeometryDto plotGeometry;

    @Schema(description = "Calculated area of the plot in hectares.", example = "1.25")
    private BigDecimal calculatedAreaHectares;

    @Schema(description = "Type of land tenure for the plot.")
    private LandTenureType landTenureType;

    @Schema(description = "UUID of the tenant this plot belongs to.")
    private UUID tenantId;

    @Schema(description = "Timestamp of when the plot was created.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;

    @Schema(description = "Timestamp of when the plot was last updated.", format = "date-time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime updatedAt;

    @Schema(description = "Version number for optimistic locking.")
    private Long version;
}