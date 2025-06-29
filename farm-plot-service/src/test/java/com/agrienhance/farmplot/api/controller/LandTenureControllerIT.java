package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.landtenure.CreateOrUpdateLandTenureRequest;
import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.entity.LandTenure;
import com.agrienhance.farmplot.domain.entity.Plot;
import com.agrienhance.farmplot.domain.enums.LandTenureType;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.agrienhance.farmplot.domain.repository.LandTenureRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public class LandTenureControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FarmRepository farmRepository;

    @Autowired
    private PlotRepository plotRepository;

    @Autowired
    private LandTenureRepository landTenureRepository;

    private UUID tenantId;
    private Farm testFarm;
    private Plot plotWithTenure;
    private Plot plotWithoutTenure;
    private LandTenure existingLandTenure;

    @BeforeEach
    void setUp() {
        landTenureRepository.deleteAllInBatch();
        plotRepository.deleteAllInBatch();
        farmRepository.deleteAllInBatch();

        tenantId = UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");

        testFarm = farmRepository.saveAndFlush(Farm.builder()
                .farmName("Parent Farm for Land Tenures")
                .ownerReferenceId(UUID.randomUUID())
                .countryCode("LT")
                .tenantId(tenantId)
                .build());

        plotWithTenure = plotRepository.saveAndFlush(Plot.builder()
                .plotName("Plot with Existing Tenure")
                .farm(testFarm)
                .tenantId(tenantId)
                .plotGeometry(createSimpleSquarePolygon(0.01, 10.0,
                        10.0))
                .build());

        plotWithoutTenure = plotRepository.saveAndFlush(Plot.builder()
                .plotName("Plot without Tenure")
                .farm(testFarm)
                .tenantId(tenantId)
                .plotGeometry(createSimpleSquarePolygon(
                        0.01, 20.0, 20.0))
                .build());

        existingLandTenure = landTenureRepository.saveAndFlush(LandTenure.builder()
                .plot(plotWithTenure)
                .tenureType(LandTenureType.OWNED)
                .ownerDetails("John Doe")
                .tenantId(tenantId)
                .build());

        // Link the tenure back to the plot for consistency in the object graph
        plotWithTenure.setLandTenureType(existingLandTenure.getTenureType());
        plotRepository.saveAndFlush(plotWithTenure);
    }

    // --- GET /v1/plots/{plotIdentifier}/land-tenure ---
    @Test
    void getLandTenure_whenExists_shouldReturn200OK() throws Exception {
        mockMvc.perform(get("/v1/plots/{plotIdentifier}/land-tenure", plotWithTenure.getPlotIdentifier())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.landTenureIdentifier", is(existingLandTenure.getLandTenureIdentifier().toString())))
                .andExpect(jsonPath("$.tenureType", is("OWNED")))
                .andExpect(jsonPath("$.ownerDetails", is("John Doe")));
    }

    @Test
    void getLandTenure_whenNotExists_shouldReturn404NotFound() throws Exception {
        mockMvc.perform(get("/v1/plots/{plotIdentifier}/land-tenure", plotWithoutTenure.getPlotIdentifier()))
                .andExpect(status().isNotFound());
    }

    // --- PUT /v1/plots/{plotIdentifier}/land-tenure ---
    @Test
    void createOrUpdateLandTenure_whenCreatingNew_shouldReturn200OK() throws Exception {
        CreateOrUpdateLandTenureRequest request = CreateOrUpdateLandTenureRequest.builder()
                .tenureType(LandTenureType.LEASED)
                .leaseStartDate(LocalDate.of(2024, 1, 1))
                .leaseEndDate(LocalDate.of(2029, 12, 31))
                .ownerDetails("Leasing Corp")
                .build();

        mockMvc.perform(put("/v1/plots/{plotIdentifier}/land-tenure", plotWithoutTenure.getPlotIdentifier())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.landTenureIdentifier", notNullValue()))
                .andExpect(jsonPath("$.tenureType", is("LEASED")))
                .andExpect(jsonPath("$.leaseStartDate", is("2024-01-01")));

        assertThat(landTenureRepository.count()).isEqualTo(2);
    }

    @Test
    void createOrUpdateLandTenure_whenUpdatingExisting_shouldReturn200OK() throws Exception {
        CreateOrUpdateLandTenureRequest request = CreateOrUpdateLandTenureRequest.builder()
                .tenureType(LandTenureType.UNKNOWN)
                .ownerDetails("Updated Owner Details")
                .build();

        mockMvc.perform(put("/v1/plots/{plotIdentifier}/land-tenure", plotWithTenure.getPlotIdentifier())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.landTenureIdentifier", is(existingLandTenure.getLandTenureIdentifier().toString())))
                .andExpect(jsonPath("$.tenureType", is("UNKNOWN")))
                .andExpect(jsonPath("$.ownerDetails", is("Updated Owner Details")));
    }

    // --- DELETE /v1/plots/{plotIdentifier}/land-tenure ---
    @Test
    void deleteLandTenure_whenExists_shouldReturn204NoContent() throws Exception {
        assertThat(landTenureRepository.existsById(existingLandTenure.getLandTenureIdentifier())).isTrue();

        mockMvc.perform(delete("/v1/plots/{plotIdentifier}/land-tenure", plotWithTenure.getPlotIdentifier()))
                .andExpect(status().isNoContent());

        assertThat(landTenureRepository.existsById(existingLandTenure.getLandTenureIdentifier())).isFalse();

        // Also verify the plot's own tenure type field was cleared
        Plot updatedPlot = plotRepository.findById(plotWithTenure.getPlotIdentifier()).orElseThrow();
        assertThat(updatedPlot.getLandTenureType()).isNull();
    }
}