package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.geojson.PolygonGeometryDto;
import com.agrienhance.farmplot.api.dto.plot.CreatePlotRequest;
import com.agrienhance.farmplot.api.dto.plot.UpdatePlotRequest;
import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.PlotRepository;
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
public class PlotControllerIT extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private FarmRepository farmRepository;

        @Autowired
        private PlotRepository plotRepository;

        private UUID tenantId;
        private Farm testFarm;
        private Plot testPlot;

        @BeforeEach
        void setUp() {
                // Clean slate for each test
                plotRepository.deleteAllInBatch();
                farmRepository.deleteAllInBatch();

                tenantId = UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");

                // A farm is required to create a plot
                testFarm = farmRepository.saveAndFlush(Farm.builder()
                                .farmName("Parent Farm for Plots")
                                .ownerReferenceId(UUID.randomUUID())
                                .countryCode("PL")
                                .tenantId(tenantId)
                                .build());

                // A pre-existing plot for GET, PUT, DELETE tests
                testPlot = plotRepository.saveAndFlush(Plot.builder()
                                .plotName("Existing Test Plot")
                                .farm(testFarm)
                                .tenantId(tenantId)
                                .plotGeometry(createSimpleSquarePolygon(0.01, 1.0,
                                                1.0))
                                .build());
        }

        // --- POST /v1/plots ---
        @Test
        void createPlot_withValidRequest_shouldReturn201Created() throws Exception {
                // Given
                CreatePlotRequest request = CreatePlotRequest.builder()
                                .farmIdentifier(testFarm.getFarmIdentifier())
                                .plotName("New API Plot")
                                .plotGeometry(createTestPolygonDto(0.01, 10.0, 10.0))
                                .build();

                // When & Then
                mockMvc.perform(post("/v1/plots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.plotIdentifier", notNullValue()))
                                .andExpect(jsonPath("$.plotName", is("New API Plot")))
                                .andExpect(jsonPath("$.farmIdentifier", is(testFarm.getFarmIdentifier().toString())))
                                .andExpect(jsonPath("$.calculatedAreaHectares", notNullValue()));

                assertThat(plotRepository.count()).isEqualTo(2); // Existing + new one
        }

        @Test
        void createPlot_withNonExistentFarm_shouldReturn404NotFound() throws Exception {
                // Given
                CreatePlotRequest request = CreatePlotRequest.builder()
                                .farmIdentifier(UUID.randomUUID()) // This farm does not exist
                                .plotName("Plot for Ghost Farm")
                                .plotGeometry(createTestPolygonDto(0.01, 20.0,
                                                20.0))
                                .build();

                // When & Then
                mockMvc.perform(post("/v1/plots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error", is("Not Found")));
        }

        @Test
        void createPlot_withInvalidRequest_missingGeometry_shouldReturn400BadRequest() throws Exception {
                // Given
                CreatePlotRequest request = CreatePlotRequest.builder()
                                .farmIdentifier(testFarm.getFarmIdentifier())
                                .plotName("Plot with no geometry")
                                .build();

                // When & Then
                mockMvc.perform(post("/v1/plots")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.validationErrors[0]",
                                                containsString("Plot geometry cannot be null")));
        }

        // --- GET /v1/plots/{plotIdentifier} ---
        @Test
        void getPlotById_whenPlotExists_shouldReturn200OK() throws Exception {
                // Given
                UUID existingPlotId = testPlot.getPlotIdentifier();

                // When & Then
                mockMvc.perform(get("/v1/plots/{plotIdentifier}", existingPlotId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.plotIdentifier", is(existingPlotId.toString())))
                                .andExpect(jsonPath("$.plotName", is("Existing Test Plot")));
        }

        @Test
        void getPlotById_whenPlotNotFound_shouldReturn404NotFound() throws Exception {
                // Given
                UUID nonExistentPlotId = UUID.randomUUID();

                // When & Then
                mockMvc.perform(get("/v1/plots/{plotIdentifier}", nonExistentPlotId))
                                .andExpect(status().isNotFound());
        }

        // --- GET /v1/plots ---
        @Test
        void listPlots_shouldReturnPaginatedPlots() throws Exception {
                // Given: testPlot exists from setUp()

                // When & Then
                mockMvc.perform(get("/v1/plots")
                                .param("page", "0")
                                .param("size", "10")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements", is(1)))
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].plotName", is("Existing Test Plot")));
        }

        @Test
        void listPlots_withFarmIdentifierFilter_shouldReturnFilteredPlots() throws Exception {
                // Given: testPlot is associated with testFarm

                // When & Then
                mockMvc.perform(get("/v1/plots")
                                .param("farmIdentifier", testFarm.getFarmIdentifier().toString())
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements", is(1)))
                                .andExpect(jsonPath("$.content[0].plotIdentifier",
                                                is(testPlot.getPlotIdentifier().toString())));
        }

        // --- PUT /v1/plots/{plotIdentifier} ---
        @Test
        void updatePlot_withValidRequest_shouldReturn200OK() throws Exception {
                // Given
                UUID existingPlotId = testPlot.getPlotIdentifier();
                UpdatePlotRequest request = UpdatePlotRequest.builder()
                                .plotName("Updated Plot Name")
                                .landTenureType(LandTenureType.LEASED)
                                .build();

                // When & Then
                mockMvc.perform(put("/v1/plots/{plotIdentifier}", existingPlotId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.plotName", is("Updated Plot Name")))
                                .andExpect(jsonPath("$.landTenureType", is("LEASED")));
        }

        @Test
        void updatePlot_whenPlotNotFound_shouldReturn404NotFound() throws Exception {
                // Given
                UUID nonExistentPlotId = UUID.randomUUID();
                UpdatePlotRequest request = UpdatePlotRequest.builder().plotName("Won't work").build();

                // When & Then
                mockMvc.perform(put("/v1/plots/{plotIdentifier}", nonExistentPlotId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // --- DELETE /v1/plots/{plotIdentifier} ---
        @Test
        void deletePlot_whenPlotExists_shouldReturn204NoContent() throws Exception {
                // Given
                UUID existingPlotId = testPlot.getPlotIdentifier();
                assertThat(plotRepository.existsById(existingPlotId)).isTrue();

                // When & Then
                mockMvc.perform(delete("/v1/plots/{plotIdentifier}", existingPlotId))
                                .andExpect(status().isNoContent());

                // Verify it's gone
                assertThat(plotRepository.existsById(existingPlotId)).isFalse();
        }

        @Test
        void deletePlot_whenPlotNotFound_shouldReturn404NotFound() throws Exception {
                // Given
                UUID nonExistentPlotId = UUID.randomUUID();

                // When & Then
                mockMvc.perform(delete("/v1/plots/{plotIdentifier}", nonExistentPlotId))
                                .andExpect(status().isNotFound());
        }
}