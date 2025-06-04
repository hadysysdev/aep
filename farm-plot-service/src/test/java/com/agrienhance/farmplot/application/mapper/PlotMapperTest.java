package com.agrienhance.farmplot.application.mapper;

import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
import com.agrienhance.farmplot.api.dto.plot.CreatePlotRequest;
import com.agrienhance.farmplot.api.dto.plot.PlotResponse;
import com.agrienhance.farmplot.api.dto.plot.UpdatePlotRequest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = { PlotMapperImpl.class, GeometryMapper.class }) // Include GeometryMapper
class PlotMapperTest {

    @Autowired
    private PlotMapper plotMapper;

    private GeometryFactory geometryFactory;
    private Farm testFarm;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        tenantId = UUID.randomUUID();
        testFarm = Farm.builder()
                .farmIdentifier(UUID.randomUUID())
                .tenantId(tenantId)
                .farmName("Test Farm for Plots")
                .build();
    }

    private Polygon createTestPolygon() {
        Coordinate[] shellCoords = {
                new Coordinate(0, 0), new Coordinate(10, 0), new Coordinate(10, 10),
                new Coordinate(0, 10), new Coordinate(0, 0)
        };
        LinearRing shell = geometryFactory.createLinearRing(shellCoords);
        return geometryFactory.createPolygon(shell, null);
    }

    private PolygonGeometryDto createTestPolygonDto() {
        List<List<Double>> exteriorRingDto = Arrays.asList(
                List.of(0.0, 0.0), List.of(10.0, 0.0), List.of(10.0, 10.0), List.of(0.0, 10.0), List.of(0.0, 0.0));
        return PolygonGeometryDto.builder()
                .type("Polygon")
                .coordinates(List.of(exteriorRingDto))
                .build();
    }

    @Test
    void shouldMapCreatePlotRequestToPlot() {
        CreatePlotRequest request = CreatePlotRequest.builder()
                .farmIdentifier(testFarm.getFarmIdentifier())
                .plotName("New Plot Alpha")
                .cultivatorReferenceId(UUID.randomUUID())
                .plotGeometry(createTestPolygonDto())
                .landTenureType(LandTenureType.OWNED)
                .tenantId(tenantId) // Matches farm's tenantId
                .build();

        Plot plot = plotMapper.createRequestToPlot(request);

        assertThat(plot).isNotNull();
        assertThat(plot.getPlotName()).isEqualTo("New Plot Alpha");
        assertThat(plot.getCultivatorReferenceId()).isEqualTo(request.getCultivatorReferenceId());
        assertThat(plot.getPlotGeometry()).isNotNull();
        assertThat(plot.getPlotGeometry().getExteriorRing().getCoordinates().length).isEqualTo(5);
        assertThat(plot.getLandTenureType()).isEqualTo(LandTenureType.OWNED); // Ensure enum is mapped
        assertThat(plot.getTenantId()).isEqualTo(request.getTenantId()); // Mapped from request

        // Ignored fields by mapper
        assertThat(plot.getFarm()).isNull(); // Farm needs to be set by service
        assertThat(plot.getPlotIdentifier()).isNull();
        assertThat(plot.getCalculatedAreaHectares()).isNull();
    }

    @Test
    void shouldMapPlotToPlotResponse() {

        BigDecimal calculatedArea = new BigDecimal(1.23);
        Plot plot = Plot.builder()
                .plotIdentifier(UUID.randomUUID())
                .farm(testFarm) // Associate with the testFarm
                .plotName("Plot Bravo")
                .cultivatorReferenceId(UUID.randomUUID())
                .plotGeometry(createTestPolygon())
                .calculatedAreaHectares(calculatedArea) // Assume this was set
                .landTenureType(LandTenureType.LEASED)
                .tenantId(testFarm.getTenantId())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .version(1L)
                .build();

        PlotResponse response = plotMapper.plotToPlotResponse(plot);

        assertThat(response).isNotNull();
        assertThat(response.getPlotIdentifier()).isEqualTo(plot.getPlotIdentifier());
        assertThat(response.getFarmIdentifier()).isEqualTo(testFarm.getFarmIdentifier()); // Check farm ID mapping
        assertThat(response.getPlotName()).isEqualTo("Plot Bravo");
        assertThat(response.getPlotGeometry().getCoordinates().get(0).get(0)).containsExactly(0.0, 0.0);
        assertThat(response.getCalculatedAreaHectares()).isEqualTo(calculatedArea);
        assertThat(response.getLandTenureType()).isEqualTo(LandTenureType.LEASED);
    }

    @Test
    void shouldUpdatePlotFromUpdatePlotRequest() {
        Plot existingPlot = Plot.builder()
                .plotIdentifier(UUID.randomUUID())
                .farm(testFarm)
                .plotName("Old Plot Name")
                .plotGeometry(createTestPolygon())
                .landTenureType(LandTenureType.OWNED)
                .tenantId(testFarm.getTenantId())
                .build();

        UpdatePlotRequest request = UpdatePlotRequest.builder()
                .plotName("New Plot Name")
                .landTenureType(LandTenureType.COMMUNAL_ACCESS)
                // plotGeometry can also be updated
                .build();

        plotMapper.updatePlotFromRequest(request, existingPlot);

        assertThat(existingPlot.getPlotName()).isEqualTo("New Plot Name");
        assertThat(existingPlot.getLandTenureType()).isEqualTo(LandTenureType.COMMUNAL_ACCESS);
        // Assert other fields that should NOT change (like tenantId, farm)
        assertThat(existingPlot.getTenantId()).isEqualTo(testFarm.getTenantId());
        assertThat(existingPlot.getFarm()).isEqualTo(testFarm);
    }
}