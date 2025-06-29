package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.UpdatePointOfInterestRequest;
import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.entity.PointOfInterest;
import com.agrienhance.farmplot.domain.enums.POIType;
import com.agrienhance.farmplot.domain.enums.ParentEntityType;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.PlotRepository;
import com.agrienhance.farmplot.domain.repository.PointOfInterestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public class PointOfInterestControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private PlotRepository plotRepository;

    @Autowired
    private PointOfInterestRepository poiRepository;

    private UUID tenantId;
    private Farm testFarm;
    private Plot testPlot;
    private PointOfInterest farmPoi;
    private PointOfInterest plotPoi;

    @BeforeEach
    void setUp() {
        poiRepository.deleteAllInBatch();
        plotRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();

        tenantId = UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");

        testFarm = farmRepository.saveAndFlush(Farm.builder()
                .farmName("Parent Farm for POIs")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("PI")
                .tenantId(tenantId)
                .build());

        testPlot = plotRepository.saveAndFlush(Plot.builder()
                .plotName("Parent Plot for POIs")
                .farm(testFarm)
                .tenantId(tenantId)
                .plotGeometry(createSimpleSquarePolygon(0.01, 30.0,
                        30.0))
                .build());

        farmPoi = poiRepository.saveAndFlush(PointOfInterest.builder()
                .poiName("Farm Well")
                .parentEntityIdentifier(testFarm.getFarmIdentifier())
                .parentEntityType(ParentEntityType.FARM)
                .poiType(POIType.WATER_SOURCE)
                .coordinates(
                        createJtsPoint(1.0, 1.0))
                .tenantId(tenantId)
                .build());

        plotPoi = poiRepository.saveAndFlush(PointOfInterest.builder()
                .poiName("Plot Scarecrow")
                .parentEntityIdentifier(testPlot.getPlotIdentifier())
                .parentEntityType(ParentEntityType.PLOT)
                .poiType(POIType.OTHER) // "Scarecrow" fits best under OTHER
                .coordinates(
                        createJtsPoint(30.5, 30.5))
                .tenantId(tenantId)
                .build());
    }

    private PointGeometryDto createPointDto(double longitude, double latitude) {
        return PointGeometryDto.builder()
                .type("Point")
                .coordinates(List.of(longitude, latitude))
                .build();
    }

    // --- POST /v1/farms/{farmIdentifier}/pois ---
    @Test
    void createPoiForFarm_shouldReturn201Created() throws Exception {
        CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
                .poiName("New Farm Gate")
                .poiType(POIType.ACCESS_POINT)
                .coordinates(createPointDto(2.0, 2.0))
                .build();

        mockMvc.perform(post("/v1/farms/{farmIdentifier}/pois", testFarm.getFarmIdentifier())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.poiIdentifier", notNullValue()))
                .andExpect(jsonPath("$.poiName", is("New Farm Gate")))
                .andExpect(jsonPath("$.parentEntityType", is("FARM")));

        assertThat(poiRepository.count()).isEqualTo(3);
    }

    // --- POST /v1/plots/{plotIdentifier}/pois ---
    @Test
    void createPoiForPlot_shouldReturn201Created() throws Exception {
        CreatePointOfInterestRequest request = CreatePointOfInterestRequest.builder()
                .poiName("New Irrigation Valve")
                .poiType(POIType.INFRASTRUCTURE)
                .coordinates(createPointDto(30.1, 30.2))
                .build();

        mockMvc.perform(post("/v1/plots/{plotIdentifier}/pois", testPlot.getPlotIdentifier())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.poiIdentifier", notNullValue()))
                .andExpect(jsonPath("$.poiName", is("New Irrigation Valve")))
                .andExpect(jsonPath("$.parentEntityType", is("PLOT")));
    }

    // --- GET /v1/farms/{farmIdentifier}/pois ---
    @Test
    void listPoisForFarm_shouldReturnFarmPois() throws Exception {
        mockMvc.perform(get("/v1/farms/{farmIdentifier}/pois", testFarm.getFarmIdentifier())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].poiName", is("Farm Well")));
    }

    // --- GET /v1/plots/{plotIdentifier}/pois ---
    @Test
    void listPoisForPlot_shouldReturnPlotPois() throws Exception {
        mockMvc.perform(get("/v1/plots/{plotIdentifier}/pois", testPlot.getPlotIdentifier())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].poiName", is("Plot Scarecrow")));
    }

    // --- GET /v1/pois/{poiIdentifier} ---
    @Test
    void getPoiById_whenExists_shouldReturn200OK() throws Exception {
        mockMvc.perform(get("/v1/pois/{poiIdentifier}", farmPoi.getPoiIdentifier())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poiName", is("Farm Well")));
    }

    @Test
    void getPoiById_whenNotFound_shouldReturn404() throws Exception {
        mockMvc.perform(get("/v1/pois/{poiIdentifier}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // --- PUT /v1/pois/{poiIdentifier} ---
    @Test
    void updatePoi_shouldReturn200OK() throws Exception {
        UpdatePointOfInterestRequest request = UpdatePointOfInterestRequest.builder()
                .poiName("Renamed Farm Well")
                .notes("This is an updated note.")
                .build();

        mockMvc.perform(put("/v1/pois/{poiIdentifier}", farmPoi.getPoiIdentifier())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poiName", is("Renamed Farm Well")))
                .andExpect(jsonPath("$.notes", is("This is an updated note.")));
    }

    // --- DELETE /v1/pois/{poiIdentifier} ---
    @Test
    void deletePoi_shouldReturn204NoContent() throws Exception {
        mockMvc.perform(delete("/v1/pois/{poiIdentifier}", plotPoi.getPoiIdentifier()))
                .andExpect(status().isNoContent());

        assertThat(poiRepository.existsById(plotPoi.getPoiIdentifier())).isFalse();
    }
}