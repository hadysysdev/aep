package com.agrienhance.farmplot.api.controller;

import com.agrienhance.farmplot.api.dto.farm.CreateFarmRequest;
import com.agrienhance.farmplot.api.dto.farm.FarmResponse;
import com.agrienhance.farmplot.api.dto.farm.UpdateFarmRequest;
// We'll need an application service later, for now, we can just define the interface or methods.
// import com.agrienhance.farmplot.application.service.FarmApplicationService; 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// import org.springframework.security.access.prepost.PreAuthorize; // For authorization later
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;
import com.agrienhance.farmplot.api.dto.poi.CreatePointOfInterestRequest;
import com.agrienhance.farmplot.api.dto.poi.PointOfInterestResponse;
import com.agrienhance.farmplot.application.service.FarmApplicationService;
import com.agrienhance.farmplot.application.service.PointOfInterestApplicationService; // Import
import com.agrienhance.farmplot.domain.enums.ParentEntityType; // Import
import org.springframework.data.domain.Page; // If using paginated list
import org.springframework.data.domain.Pageable; // If using paginated list

import java.net.URI;
import java.util.List; // If using non-paginated list

@RestController
@RequestMapping("/v1/farms")
@Tag(name = "Farm Management", description = "APIs for managing farms")
@AllArgsConstructor
public class FarmController {

        // Placeholder for the application service - to be implemented
        private final FarmApplicationService farmApplicationService;
        private final PointOfInterestApplicationService poiApplicationService; // Add this

        private UUID getAuthenticatedTenantId() {
                // TODO: Replace with actual logic to extract tenantId from Spring Security
                // context
                // For now, we return a hardcoded UUID for testing purposes.
                // This MUST be replaced before going to production.
                return UUID.fromString("a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5");
        }

        @Operation(summary = "Register a new farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Farm created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FarmResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data")
        })
        @PostMapping
        public ResponseEntity<FarmResponse> registerFarm(@Valid @RequestBody CreateFarmRequest createFarmRequest) {
                // In a real app, we'd also validate that createFarmRequest.getTenantId()
                // matches the authenticated user's tenant
                FarmResponse createdFarm = farmApplicationService.createFarm(createFarmRequest,
                                getAuthenticatedTenantId());

                // Build the location URI for the newly created resource
                URI location = ServletUriComponentsBuilder
                                .fromCurrentRequest()
                                .path("/{farmIdentifier}")
                                .buildAndExpand(createdFarm.getFarmIdentifier())
                                .toUri();

                return ResponseEntity.created(location).body(createdFarm);
        }

        @Operation(summary = "Get farm details by its identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Farm details retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FarmResponse.class))),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @GetMapping("/{farmIdentifier}")
        public ResponseEntity<FarmResponse> getFarmById(
                        @Parameter(description = "UUID of the farm to retrieve") @PathVariable UUID farmIdentifier) {
                // Use our temporary method to get the tenantId
                UUID tenantId = getAuthenticatedTenantId();
                FarmResponse farm = farmApplicationService.getFarmById(farmIdentifier, tenantId);
                return ResponseEntity.ok(farm);
        }

        @Operation(summary = "List all farms for the current tenant (paginated)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of farms retrieved")
        })
        @GetMapping
        public ResponseEntity<Page<FarmResponse>> listFarms(
                        @Parameter(description = "Pagination and sorting parameters") @PageableDefault(size = 20, sort = "farmName") Pageable pageable) {
                UUID tenantId = getAuthenticatedTenantId();
                Page<FarmResponse> farms = farmApplicationService.listFarmsByTenant(tenantId, pageable);
                return ResponseEntity.ok(farms);
        }

        @Operation(summary = "Update an existing farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Farm updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = FarmResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @PutMapping("/{farmIdentifier}")
        public ResponseEntity<FarmResponse> updateFarm(
                        @Parameter(description = "UUID of the farm to update") @PathVariable UUID farmIdentifier,
                        @Valid @RequestBody UpdateFarmRequest updateFarmRequest) {
                UUID tenantId = getAuthenticatedTenantId();
                FarmResponse updatedFarm = farmApplicationService.updateFarm(farmIdentifier, updateFarmRequest,
                                tenantId);
                return ResponseEntity.ok(updatedFarm);
        }

        @Operation(summary = "Delete a farm by its identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Farm deleted successfully"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @DeleteMapping("/{farmIdentifier}")
        public ResponseEntity<Void> deleteFarm(
                        @Parameter(description = "UUID of the farm to delete") @PathVariable UUID farmIdentifier) {
                UUID tenantId = getAuthenticatedTenantId();
                farmApplicationService.deleteFarm(farmIdentifier, tenantId);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Create a Point Of Interest for a specific farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "POI created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PointOfInterestResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @PostMapping("/{farmIdentifier}/pois")
        public ResponseEntity<PointOfInterestResponse> createFarmPoi(
                        @Parameter(description = "UUID of the farm") @PathVariable UUID farmIdentifier,
                        @Valid @RequestBody CreatePointOfInterestRequest request) {
                // UUID tenantId = ... from security context ...
                UUID tenantId = getAuthenticatedTenantId();
                PointOfInterestResponse createdPoi = poiApplicationService.createPoi(
                                farmIdentifier,
                                ParentEntityType.FARM,
                                tenantId,
                                request);
                return new ResponseEntity<>(createdPoi, HttpStatus.CREATED);
        }

        @Operation(summary = "List all Points Of Interest for a specific farm")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "List of POIs retrieved"),
                        @ApiResponse(responseCode = "404", description = "Farm not found")
        })
        @GetMapping("/{farmIdentifier}/pois")
        public ResponseEntity<List<PointOfInterestResponse>> listFarmPois( // Or Page<PointOfInterestResponse> with
                                                                           // Pageable
                        @Parameter(description = "UUID of the farm") @PathVariable UUID farmIdentifier) {
                // UUID tenantId = ... from security context ...
                UUID tenantId = getAuthenticatedTenantId();
                // If using Pageable: Page<PointOfInterestResponse> pois =
                // poiApplicationService.listPoisByParentPaginated(farmIdentifier,
                // ParentEntityType.FARM, tenantId, pageable);
                List<PointOfInterestResponse> pois = poiApplicationService.listPoisByParent(farmIdentifier,
                                ParentEntityType.FARM, tenantId);
                return ResponseEntity.ok(pois);
        }
}