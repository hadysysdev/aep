package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.UpdateFarmRequest;
import com.agrienhance.farmplot.api.dto.geojson.PointGeometryDto;
import com.agrienhance.farmplot.config.AbstractIntegrationTest;
import com.agrienhance.farmplot.domain.entity.Farm;
import com.agrienhance.farmplot.domain.repository.FarmRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public class FarmControllerIT extends AbstractIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private FarmRepository farmRepository;

        private UUID tenantId;
        private UUID ownerId;
        private Farm testFarm; // To store a pre-existing farm for tests

        @BeforeEach
        void setUp() {
                farmRepository.deleteAllInBatch();
                // Use the SAME hardcoded tenantId as the one in the controller's placeholder
                // method
                tenantId = UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");
                ownerId = UUID.randomUUID();

                // Create a farm that can be used for GET, PUT, DELETE tests
                testFarm = farmRepository.saveAndFlush(Farm.builder()
                                .farmName("Existing Test Farm")
                                .ownerReferenceId(ownerId)
                                .countryCode("XT") // Existing Test country
                                .region("Existing Region")
                                .tenantId(tenantId)
                                .generalLocationCoordinates(geometryFactory.createPoint(new Coordinate(1.0, 1.0)))
                                .build());
        }

        // --- POST /v1/farms ---
        @Test
        void registerFarm_withValidRequest_shouldReturn201CreatedAndFarmResponse() throws Exception {
                // Given
                CreateFarmRequest request = CreateFarmRequest.builder()
                                .farmName("API Test Farm")
                                .ownerReferenceId(ownerId)
                                .countryCode("AI")
                                .build();

                // When & Then
                mockMvc.perform(post("/v1/farms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.farmIdentifier", notNullValue()))
                                .andExpect(jsonPath("$.farmName", is("API Test Farm")));

                assertThat(farmRepository.count()).isEqualTo(2); // Existing + new one
        }

        @Test
        void registerFarm_withInvalidRequest_missingFarmName_shouldReturn400BadRequest() throws Exception {
                // Given
                CreateFarmRequest request = CreateFarmRequest.builder()
                                .ownerReferenceId(ownerId)
                                .countryCode("BD")
                                .build();

                // When & Then
                mockMvc.perform(post("/v1/farms")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error", is("Bad Request")))
                                .andExpect(jsonPath("$.validationErrors[0]",
                                                containsString("Farm name cannot be blank")));
        }

        // --- GET /v1/farms/{farmIdentifier} ---
        @Test
        void getFarmById_whenFarmExists_shouldReturn200OKAndFarmResponse() throws Exception {
                // Given
                UUID existingFarmId = testFarm.getFarmIdentifier();

                // When & Then
                mockMvc.perform(get("/v1/farms/{farmIdentifier}", existingFarmId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Content-Type", is(MediaType.APPLICATION_JSON_VALUE)))
                                .andExpect(jsonPath("$.farmIdentifier", is(existingFarmId.toString())))
                                .andExpect(jsonPath("$.farmName", is("Existing Test Farm")));
        }

        @Test
        void getFarmById_whenFarmNotFound_shouldReturn404NotFound() throws Exception {
                // Given
                UUID nonExistentFarmId = UUID.randomUUID();

                // When & Then
                mockMvc.perform(get("/v1/farms/{farmIdentifier}", nonExistentFarmId)
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.error", is("Not Found")))
                                .andExpect(jsonPath("$.message", containsString("Farm with identifier")));
        }

        // --- GET /v1/farms ---
        @Test
        void listFarms_shouldReturn200OKAndPaginatedFarms() throws Exception {
                // Given: testFarm already exists from setUp()

                // When & Then
                mockMvc.perform(get("/v1/farms")
                                .param("page", "0")
                                .param("size", "5")
                                .param("sort", "farmName,asc")
                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements", is(1)))
                                .andExpect(jsonPath("$.totalPages", is(1)))
                                .andExpect(jsonPath("$.content", hasSize(1)))
                                .andExpect(jsonPath("$.content[0].farmName", is("Existing Test Farm")));
        }

        // --- PUT /v1/farms/{farmIdentifier} ---
        @Test
        void updateFarm_withValidRequest_shouldReturn200OKAndUpdatedFarmResponse() throws Exception {
                // Given
                UUID existingFarmId = testFarm.getFarmIdentifier();
                UpdateFarmRequest request = UpdateFarmRequest.builder()
                                .farmName("Updated Farm Name via API")
                                .region("Updated Region")
                                .build();

                // When & Then
                mockMvc.perform(put("/v1/farms/{farmIdentifier}", existingFarmId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.farmName", is("Updated Farm Name via API")))
                                .andExpect(jsonPath("$.region", is("Updated Region")))
                                .andExpect(jsonPath("$.countryCode", is("XT"))); // Check that non-updated field is
                                                                                 // preserved
        }

        @Test
        void updateFarm_whenFarmNotFound_shouldReturn404NotFound() throws Exception {
                // Given
                UUID nonExistentFarmId = UUID.randomUUID();
                UpdateFarmRequest request = UpdateFarmRequest.builder().farmName("Won't work").build();

                // When & Then
                mockMvc.perform(put("/v1/farms/{farmIdentifier}", nonExistentFarmId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        // --- DELETE /v1/farms/{farmIdentifier} ---
        @Test
        void deleteFarm_whenFarmExists_shouldReturn204NoContent() throws Exception {
                // Given
                UUID existingFarmId = testFarm.getFarmIdentifier();
                assertThat(farmRepository.existsById(existingFarmId)).isTrue();

                // When & Then
                mockMvc.perform(delete("/v1/farms/{farmIdentifier}", existingFarmId))
                                .andExpect(status().isNoContent());

                // Verify it's gone from the database
                assertThat(farmRepository.existsById(existingFarmId)).isFalse();
        }

        @Test
        void deleteFarm_whenFarmNotFound_shouldReturn404NotFound() throws Exception {
                // Given
                UUID nonExistentFarmId = UUID.randomUUID();

                // When & Then
                mockMvc.perform(delete("/v1/farms/{farmIdentifier}", nonExistentFarmId))
                                .andExpect(status().isNotFound());
        }
}