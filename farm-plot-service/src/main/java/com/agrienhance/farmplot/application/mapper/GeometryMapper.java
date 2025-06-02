package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
import org.locationtech.jts.geom.*;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component; // To make it a Spring bean if needed, or use @Mapper(componentModel = "spring")

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component // Make it a Spring component so it can be injected/used by other mappers
// Or use @Mapper(componentModel = "spring") if you prefer MapStruct's way
public class GeometryMapper {

    private final GeometryFactory geometryFactory;

    public GeometryMapper() {
        // SRID 4326 corresponds to WGS 84
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    // --- Point Mapping ---
    public Point toPoint(PointGeometryDto dto) {
        if (dto == null || dto.getCoordinates() == null || dto.getCoordinates().size() < 2) {
            return null;
        }
        // GeoJSON order is [longitude, latitude]
        // JTS Coordinate order is (x, y) which typically means (longitude, latitude)
        return geometryFactory.createPoint(new Coordinate(dto.getCoordinates().get(0), dto.getCoordinates().get(1)));
    }

    public PointGeometryDto toPointGeometryDto(Point point) {
        if (point == null) {
            return null;
        }
        return PointGeometryDto.builder()
                .type("Point")
                .coordinates(List.of(point.getX(), point.getY()))
                .build();
    }

    // --- Polygon Mapping ---
    public Polygon toPolygon(PolygonGeometryDto dto) {
        if (dto == null || dto.getCoordinates() == null || dto.getCoordinates().isEmpty()) {
            return null;
        }

        List<List<List<Double>>> dtoRings = dto.getCoordinates();
        LinearRing exteriorRing = null;
        List<LinearRing> interiorRings = new ArrayList<>();

        for (int i = 0; i < dtoRings.size(); i++) {
            List<List<Double>> dtoRing = dtoRings.get(i);
            Coordinate[] coords = dtoRing.stream()
                    .map(point -> new Coordinate(point.get(0), point.get(1)))
                    .toArray(Coordinate[]::new);

            if (coords.length > 0 && !coords[0].equals(coords[coords.length - 1])) {
                // GeoJSON spec doesn't require last point to be same as first for DTOs,
                // but JTS LinearRing does. For simplicity, we assume valid input or handle it.
                // For robust parsing, ensure the ring is closed.
                Coordinate[] closedCoords = Arrays.copyOf(coords, coords.length + 1);
                closedCoords[coords.length] = coords[0]; // Ensure closure for JTS
                coords = closedCoords;
            }

            if (coords.length < 4 && dtoRings.size() == 1 && i == 0) { // A linear ring needs at least 4 points (3
                                                                       // unique, last same as first)
                // If it's the only ring and malformed, return null or throw exception
                return null;
            } else if (coords.length < 4) {
                // Skip malformed interior rings or throw
                continue;
            }

            if (i == 0) { // First ring is the exterior shell
                exteriorRing = geometryFactory.createLinearRing(coords);
            } else { // Subsequent rings are interior holes
                interiorRings.add(geometryFactory.createLinearRing(coords));
            }
        }

        if (exteriorRing == null) {
            return null; // Or throw an exception for invalid Polygon DTO
        }
        return geometryFactory.createPolygon(exteriorRing, interiorRings.toArray(new LinearRing[0]));
    }

    public PolygonGeometryDto toPolygonGeometryDto(Polygon polygon) {
        if (polygon == null) {
            return null;
        }

        List<List<List<Double>>> allRingsDto = new ArrayList<>();

        // Exterior Ring
        LinearRing exteriorRing = polygon.getExteriorRing();
        List<List<Double>> exteriorRingDto = Arrays.stream(exteriorRing.getCoordinates())
                .map(c -> List.of(c.getX(), c.getY()))
                .collect(Collectors.toList());
        allRingsDto.add(exteriorRingDto);

        // Interior Rings (Holes)
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            LinearRing interiorRing = polygon.getInteriorRingN(i);
            List<List<Double>> interiorRingDto = Arrays.stream(interiorRing.getCoordinates())
                    .map(c -> List.of(c.getX(), c.getY()))
                    .collect(Collectors.toList());
            allRingsDto.add(interiorRingDto);
        }

        return PolygonGeometryDto.builder()
                .type("Polygon")
                .coordinates(allRingsDto)
                .build();
    }
}