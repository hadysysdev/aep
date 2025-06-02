package com.agrienhance.farmplot.api.dto.geojson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Represents a GeoJSON Polygon geometry. Coordinates are a list of linear rings (the first being the exterior ring, subsequent ones being interior rings/holes).")
public class PolygonGeometryDto {

    @NotNull
    @Pattern(regexp = "Polygon", message = "Type must be 'Polygon'")
    @Schema(defaultValue = "Polygon", description = "The type of GeoJSON object.", example = "Polygon")
    @Builder.Default
    private String type = "Polygon";

    @NotNull
    @Size(min = 1, message = "Polygon coordinates must contain at least one linear ring (the exterior ring).")
    @Schema(description = "A list of linear ring coordinate arrays. The first ring is the exterior boundary. Subsequent rings are interior boundaries (holes). Each linear ring is a list of [longitude, latitude] points, where the first and last points are identical.", example = "[[[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]]]") // Example
                                                                                                                                                                                                                                                                                                                                                                  // of
                                                                                                                                                                                                                                                                                                                                                                  // a
                                                                                                                                                                                                                                                                                                                                                                  // single
                                                                                                                                                                                                                                                                                                                                                                  // exterior
                                                                                                                                                                                                                                                                                                                                                                  // //
                                                                                                                                                                                                                                                                                                                                                                  // ring
    private List<List<List<Double>>> coordinates; // List of rings -> List of points -> List of coords (lon, lat)
}