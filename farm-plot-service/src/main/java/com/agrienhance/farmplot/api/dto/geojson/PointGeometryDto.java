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
@Schema(description = "Represents a GeoJSON Point geometry.")
public class PointGeometryDto {

    @NotNull
    @Pattern(regexp = "Point", message = "Type must be 'Point'") // Ensure it's always "Point"
    @Schema(defaultValue = "Point", description = "The type of GeoJSON object.", example = "Point")
    @Builder.Default
    private String type = "Point";

    @NotNull
    @Size(min = 2, max = 3, message = "Coordinates must contain 2 (longitude, latitude) or 3 (longitude, latitude, altitude) elements.")
    @Schema(description = "Array of coordinates [longitude, latitude, optional_altitude].", example = "[36.70, -1.28]")
    private List<Double> coordinates;
}