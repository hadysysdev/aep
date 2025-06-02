package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { GeometryMapper.class })
class GeometryMapperTest {

    @Autowired
    private GeometryMapper geometryMapper;

    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326); // WGS84
    }

    // --- Point Tests ---
    @Test
    void shouldMapPointGeometryDtoToPoint() {
        PointGeometryDto dto = PointGeometryDto.builder().type("Point").coordinates(List.of(10.5, 20.5)).build();
        Point point = geometryMapper.toPoint(dto);

        assertThat(point).isNotNull();
        assertThat(point.getX()).isEqualTo(10.5);
        assertThat(point.getY()).isEqualTo(20.5);
        assertThat(point.getSRID()).isEqualTo(4326);
    }

    @Test
    void shouldMapPointToPointGeometryDto() {
        Point point = geometryFactory.createPoint(new Coordinate(10.5, 20.5));
        PointGeometryDto dto = geometryMapper.toPointGeometryDto(point);

        assertThat(dto).isNotNull();
        assertThat(dto.getType()).isEqualTo("Point");
        assertThat(dto.getCoordinates()).containsExactly(10.5, 20.5);
    }

    @Test
    void toPoint_shouldReturnNull_whenDtoIsNull() {
        assertThat(geometryMapper.toPoint(null)).isNull();
    }

    @Test
    void toPointGeometryDto_shouldReturnNull_whenPointIsNull() {
        assertThat(geometryMapper.toPointGeometryDto(null)).isNull();
    }

    // --- Polygon Tests ---
    @Test
    void shouldMapPolygonGeometryDtoToPolygon() {
        List<List<Double>> exteriorRingDto = Arrays.asList(
                List.of(0.0, 0.0), List.of(10.0, 0.0), List.of(10.0, 10.0), List.of(0.0, 10.0), List.of(0.0, 0.0));
        PolygonGeometryDto dto = PolygonGeometryDto.builder()
                .type("Polygon")
                .coordinates(List.of(exteriorRingDto)) // Single exterior ring
                .build();

        Polygon polygon = geometryMapper.toPolygon(dto);

        assertThat(polygon).isNotNull();
        assertThat(polygon.getExteriorRing().getCoordinates()).hasSize(5);
        assertThat(polygon.getExteriorRing().getCoordinateN(0)).isEqualTo(new Coordinate(0.0, 0.0));
        assertThat(polygon.getNumInteriorRing()).isEqualTo(0);
        assertThat(polygon.getSRID()).isEqualTo(4326);
    }

    @Test
    void shouldMapPolygonToPolygonGeometryDto() {
        Coordinate[] shellCoords = {
                new Coordinate(0, 0), new Coordinate(10, 0), new Coordinate(10, 10),
                new Coordinate(0, 10), new Coordinate(0, 0)
        };
        LinearRing shell = geometryFactory.createLinearRing(shellCoords);
        Polygon polygon = geometryFactory.createPolygon(shell, null); // No holes

        PolygonGeometryDto dto = geometryMapper.toPolygonGeometryDto(polygon);

        assertThat(dto).isNotNull();
        assertThat(dto.getType()).isEqualTo("Polygon");
        assertThat(dto.getCoordinates()).hasSize(1); // One ring (exterior)
        assertThat(dto.getCoordinates().get(0)).hasSize(5);
        assertThat(dto.getCoordinates().get(0).get(0)).containsExactly(0.0, 0.0);
    }

    @Test
    void toPolygon_shouldMapPolygonWithHole() {
        List<List<Double>> exteriorRingDto = Arrays.asList(
                List.of(0.0, 0.0), List.of(10.0, 0.0), List.of(10.0, 10.0), List.of(0.0, 10.0), List.of(0.0, 0.0));
        List<List<Double>> interiorRingDto = Arrays.asList(
                List.of(1.0, 1.0), List.of(1.0, 2.0), List.of(2.0, 2.0), List.of(2.0, 1.0), List.of(1.0, 1.0));
        PolygonGeometryDto dto = PolygonGeometryDto.builder()
                .type("Polygon")
                .coordinates(Arrays.asList(exteriorRingDto, interiorRingDto))
                .build();

        Polygon polygon = geometryMapper.toPolygon(dto);

        assertThat(polygon).isNotNull();
        assertThat(polygon.getExteriorRing().getCoordinates()).hasSize(5);
        assertThat(polygon.getNumInteriorRing()).isEqualTo(1);
        assertThat(polygon.getInteriorRingN(0).getCoordinates()).hasSize(5);
    }

    @Test
    void toPolygon_shouldReturnNull_whenDtoIsNull() {
        assertThat(geometryMapper.toPolygon(null)).isNull();
    }

    @Test
    void toPolygonGeometryDto_shouldReturnNull_whenPolygonIsNull() {
        assertThat(geometryMapper.toPolygonGeometryDto(null)).isNull();
    }
}